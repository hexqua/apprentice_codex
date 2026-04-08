package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SearchBeaconState;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.UUID;

public class SearchBeaconEntity extends PathfinderMob implements GeoEntity {
    public static final float WIDTH = 0.7f;
    public static final float HEIGHT = 1.45f;
    private static final int SEARCH_RESOLVE_TICKS = 20 * 3;
    private static final int OFFER_CHECK_INTERVAL_TICKS = 4;
    private static final int DIRECTION_PARTICLE_INTERVAL_TICKS = 8;
    private static final int REOFFER_COOLDOWN_TICKS = 20 * 5;
    private static final double OFFER_PICKUP_RANGE = 0.85;
    private static final double MIN_HORIZONTAL_DISTANCE = 0.1;
    private static final double RESULT_DISCARD_DISTANCE_SQR = 32.0 * 32.0;
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final float RHOMBUS_SIZE = 0.22f;
    private static final float SPARK_SIZE = 0.12f;
    private static final float UNKNOWN_RED = 0.95f;
    private static final float UNKNOWN_GREEN = 0.80f;
    private static final float UNKNOWN_BLUE = 0.22f;
    private static final float SEARCHED_RED = 0.32f;
    private static final float SEARCHED_GREEN = 0.66f;
    private static final float SEARCHED_BLUE = 1.0f;
    private static final float TRAVERSED_RED = 1.0f;
    private static final float TRAVERSED_GREEN = 0.28f;
    private static final float TRAVERSED_BLUE = 0.22f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID ownerUuid;
    private @Nullable ServerPlayer cachedOwner;
    private Phase phase = Phase.IDLE;
    private int phaseTicks;
    private double anchorX;
    private double anchorY;
    private double anchorZ;
    private int initialRange;
    private int additionalRangePerItem;
    private int searchRange;
    private ItemStack offeredItem = ItemStack.EMPTY;
    private String targetLabel = "";
    private @Nullable net.minecraft.resources.ResourceLocation ignoredOfferItemId;
    private int ignoredOfferUntilTick;
    private @Nullable SearchBeaconSearchService.SearchSession searchSession;
    private @Nullable SearchBeaconSearchService.SearchResult searchResult;

    public SearchBeaconEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
        setNoGravity(true);
        noPhysics = true;
        setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    public void setOwner(ServerPlayer owner) {
        ownerUuid = owner.getUUID();
        cachedOwner = owner;
    }

    public void setAnchor(Vec3 anchor) {
        anchorX = anchor.x;
        anchorY = anchor.y;
        anchorZ = anchor.z;
        setPos(anchorX, anchorY, anchorZ);
    }

    public void setSearchTuning(int initialRange, int additionalRangePerItem) {
        this.initialRange = Math.max(0, initialRange);
        this.additionalRangePerItem = Math.max(0, additionalRangePerItem);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }

        keepAnchored();

        switch (phase) {
            case IDLE -> tickIdle(serverLevel);
            case ARMED -> tickArmed();
            case SEARCHING -> tickSearching(serverLevel);
            case RESULT -> tickResult(serverLevel);
        }

        phaseTicks++;
    }

    private void keepAnchored() {
        setNoGravity(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        if (distanceToSqr(anchorX, anchorY, anchorZ) > 0.0001) {
            setPos(anchorX, anchorY, anchorZ);
        }
    }

    private void tickIdle(ServerLevel level) {
        if (tickCount % OFFER_CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        var nearbyItem = level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(OFFER_PICKUP_RANGE), item -> !item.isRemoved())
                .stream()
                .min(Comparator.comparingDouble(item -> item.distanceToSqr(this)))
                .orElse(null);
        if (nearbyItem == null) {
            return;
        }

        absorbOfferedItem(nearbyItem);
    }

    private void tickArmed() {
    }

    private void tickSearching(ServerLevel level) {
        if (searchSession != null && !searchSession.isComplete()) {
            searchSession.advance(level, SearchBeaconSearchService.getSearchStepsPerTick());
            if (searchSession.isComplete()) {
                searchResult = searchSession.getResult();
            }
        }

        if (phaseTicks >= SEARCH_RESOLVE_TICKS && searchSession != null && searchSession.isComplete()) {
            revealSearchResult();
        }
    }

    private void tickResult(ServerLevel level) {
        if (searchResult != null && !searchResult.isEmpty() && phaseTicks % DIRECTION_PARTICLE_INTERVAL_TICKS == 0) {
            emitDirectionParticles(level, searchResult);
        }

        var owner = getOwner();
        if (owner != null && owner.distanceToSqr(this) > RESULT_DISCARD_DISTANCE_SQR) {
            finishAndDiscard();
        }
    }

    private void absorbOfferedItem(ItemEntity itemEntity) {
        var offeredStack = itemEntity.getItem();
        var itemId = ForgeRegistries.ITEMS.getKey(offeredStack.getItem());
        if (itemId != null && itemId.equals(ignoredOfferItemId) && tickCount < ignoredOfferUntilTick) {
            return;
        }

        var definition = SearchBeaconTargetManager.getDefinition(offeredStack);
        if (definition == null) {
            return;
        }

        var acceptedCount = getAcceptedItemCount(offeredStack.getCount());
        if (acceptedCount <= 0) {
            return;
        }

        offeredItem = offeredStack.copyWithCount(acceptedCount);
        targetLabel = SearchBeaconTargetManager.createDisplayLabel(definition);
        sendOwnerActionBar(Component.translatable(
                "ui.apprenticecodex.search_beacon.entity.target_by_item",
                offeredItem.getDisplayName(),
                Component.literal(targetLabel)
        ).withStyle(ChatFormatting.YELLOW));

        var remainingCount = offeredStack.getCount() - acceptedCount;
        if (remainingCount > 0) {
            itemEntity.setItem(offeredStack.copyWithCount(remainingCount));
        } else {
            itemEntity.discard();
        }

        searchRange = getSearchRangeForCount(acceptedCount);
        searchSession = null;
        searchResult = null;
        transitionTo(Phase.ARMED);
    }

    private void startSearch(ServerLevel level) {
        if (phase != Phase.ARMED || offeredItem.isEmpty()) {
            return;
        }

        var definition = SearchBeaconTargetManager.getDefinition(offeredItem);
        if (definition == null) {
            transitionTo(Phase.IDLE);
            offeredItem = ItemStack.EMPTY;
            targetLabel = "";
            searchRange = 0;
            searchSession = null;
            return;
        }

        searchSession = SearchBeaconSearchService.createSession(
                level,
                blockPosition(),
                searchRange,
                definition,
                resolveSpellState()
        );
        searchResult = searchSession.getResult();
        sendOwnerActionBar(Component.translatable(
                "ui.apprenticecodex.search_beacon.entity.search_start",
                Component.literal(targetLabel),
                searchRange
        ).withStyle(ChatFormatting.YELLOW));
        level.playSound(null, blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.8f, 1.15f);
        transitionTo(Phase.SEARCHING);
    }

    private SearchBeaconState resolveSpellState() {
        var owner = getOwner();
        if (owner == null) {
            return new SearchBeaconState();
        }

        return owner.getCapability(Capabilities.SPELL_DATA)
                .map(capability -> capability.get(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE))
                .orElse(new SearchBeaconState());
    }

    private void revealSearchResult() {
        if (phase != Phase.SEARCHING) {
            return;
        }

        var result = searchResult != null ? searchResult : new SearchBeaconSearchService.SearchResult(java.util.List.of());
        if (result.isEmpty()) {
            sendOwnerActionBar(Component.translatable(
                    "ui.apprenticecodex.search_beacon.entity.not_found",
                    Component.literal(targetLabel),
                    searchRange
            ).withStyle(ChatFormatting.RED));
        } else if (result.hasUnknownStructures()) {
            sendOwnerActionBar(Component.translatable(
                    "ui.apprenticecodex.search_beacon.entity.found",
                    Component.literal(targetLabel),
                    searchRange
            ).withStyle(ChatFormatting.GREEN));
        } else {
            sendOwnerActionBar(Component.translatable(
                    "ui.apprenticecodex.search_beacon.entity.only_found",
                    Component.literal(targetLabel),
                    searchRange
            ).withStyle(ChatFormatting.YELLOW));
        }

        var owner = getOwner();
        if (owner != null && !result.isEmpty()) {
            Capabilities.withSpellData(owner, data -> {
                var state = data.get(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE);
                if (state.markSearched(result.foundStructureIds())) {
                    data.markDirty(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE.id());
                }
            });
        }

        transitionTo(Phase.RESULT);
    }

    private void emitDirectionParticles(ServerLevel level, SearchBeaconSearchService.SearchResult result) {
        var base = position().add(0.0, 1.05, 0.0);
        for (var located : result.locatedStructures()) {
            var dx = located.center().getX() + 0.5 - base.x;
            var dz = located.center().getZ() + 0.5 - base.z;
            var horizontalLength = Math.sqrt(dx * dx + dz * dz);
            if (horizontalLength < MIN_HORIZONTAL_DISTANCE) {
                continue;
            }

            var dirX = dx / horizontalLength;
            var dirZ = dz / horizontalLength;
            var colors = getParticleColors(located.knowledge());
            for (int i = 0; i < 3; i++) {
                var distance = 0.9 + i * 0.75;
                var x = base.x + dirX * distance;
                var y = base.y + 0.05 + i * 0.08;
                var z = base.z + dirZ * distance;
                level.sendParticles(
                        new AdditiveGlowParticleOptions(
                                ParticleRegistry.ADDITIVE_RHOMBUS.get(),
                                RHOMBUS_SIZE,
                                colors.red(),
                                colors.green(),
                                colors.blue(),
                                3
                        ),
                        x, y, z,
                        1,
                        dirX * 0.01,
                        0.02,
                        dirZ * 0.01,
                        0.0
                );
            }

            for (int i = 0; i < 5; i++) {
                var distance = 0.65 + i * 0.45;
                var x = base.x + dirX * distance;
                var y = base.y + 0.12;
                var z = base.z + dirZ * distance;
                level.sendParticles(
                        new AdditiveGlowParticleOptions(
                                ParticleRegistry.ADDITIVE_SPARK.get(),
                                SPARK_SIZE,
                                colors.red(),
                                colors.green(),
                                colors.blue(),
                                1
                        ),
                        x, y, z,
                        1,
                        dirX * 0.02,
                        0.01,
                        dirZ * 0.02,
                        0.0
                );
            }
        }
    }

    private void finishAndDiscard() {
        var shouldRefund = searchResult == null || searchResult.isEmpty() || !searchResult.hasUnknownStructures();
        if (shouldRefund && !offeredItem.isEmpty()) {
            var refundCount = Mth.floor(offeredItem.getCount() / 2.0f);
            if (refundCount > 0) {
                spawnAtLocation(offeredItem.copyWithCount(refundCount));
            }
        }
        clearOfferState();
        searchSession = null;
        discard();
    }

    private void returnOfferedItemToOwner(ServerPlayer owner) {
        if (offeredItem.isEmpty()) {
            return;
        }

        var returningStack = offeredItem.copy();
        owner.addItem(returningStack);
        if (!returningStack.isEmpty()) {
            // 回収直後の足元ドロップを即再吸収すると、取り出し操作の意味がなくなる。
            // Forge/Minecraft の item pickup 周りは loader 更新で変わりやすいため、ここでは item id 単位で短時間だけ受け取りを止める。
            var dropped = spawnAtLocation(returningStack);
            if (dropped != null) {
                dropped.setPickUpDelay(0);
                ignoredOfferItemId = ForgeRegistries.ITEMS.getKey(returningStack.getItem());
                ignoredOfferUntilTick = tickCount + REOFFER_COOLDOWN_TICKS;
            }
        } else {
            ignoredOfferItemId = null;
            ignoredOfferUntilTick = 0;
        }

        clearOfferState();
        transitionTo(Phase.IDLE);
    }

    private void clearOfferState() {
        offeredItem = ItemStack.EMPTY;
        targetLabel = "";
        searchRange = 0;
        searchResult = null;
        searchSession = null;
    }

    private int getAcceptedItemCount(int offeredCount) {
        if (offeredCount <= 0) {
            return 0;
        }
        var maxSearchRange = SearchBeaconSearchService.getMaxSearchRange();
        if (maxSearchRange <= 0) {
            return 0;
        }
        // 1 個目は初期距離だけを使い、追加距離は 2 個目以降でだけ伸ばす。
        if (additionalRangePerItem <= 0 || initialRange >= maxSearchRange) {
            return 1;
        }

        // 上限までの残り距離が追加距離 1 回分に満たなくても、1 個で上限へ届くなら受け付ける。
        var remainingRange = Math.max(0, maxSearchRange - initialRange);
        var maxExtraItems = Mth.positiveCeilDiv(remainingRange, additionalRangePerItem);
        return Math.min(offeredCount, 1 + maxExtraItems);
    }

    private int getSearchRangeForCount(int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }
        if (itemCount == 1) {
            return SearchBeaconSearchService.clampRange(initialRange);
        }
        return SearchBeaconSearchService.clampRange(initialRange + additionalRangePerItem * (itemCount - 1));
    }

    private static ParticleColors getParticleColors(SearchBeaconState.StructureKnowledge knowledge) {
        return switch (knowledge) {
            case SEARCHED -> new ParticleColors(SEARCHED_RED, SEARCHED_GREEN, SEARCHED_BLUE);
            case TRAVERSED -> new ParticleColors(TRAVERSED_RED, TRAVERSED_GREEN, TRAVERSED_BLUE);
            case UNKNOWN -> new ParticleColors(UNKNOWN_RED, UNKNOWN_GREEN, UNKNOWN_BLUE);
        };
    }

    private void transitionTo(Phase nextPhase) {
        phase = nextPhase;
        phaseTicks = 0;
    }

    private @Nullable ServerPlayer getOwner() {
        if (cachedOwner != null && cachedOwner.isAlive() && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }
        if (ownerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        cachedOwner = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
        return cachedOwner;
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        var owner = getOwner();
        if (hand != InteractionHand.MAIN_HAND || player != owner) {
            return InteractionResult.PASS;
        }

        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (phase == Phase.IDLE) {
            var heldStack = player.getItemInHand(hand);
            if (heldStack.isEmpty()) {
                return InteractionResult.PASS;
            }

            var definition = SearchBeaconTargetManager.getDefinition(heldStack);
            if (definition == null) {
                sendOwnerActionBar(Component.translatable(
                        "ui.apprenticecodex.search_beacon.entity.not_target_item",
                        heldStack.getDisplayName()
                ).withStyle(ChatFormatting.RED));
            } else {
                sendOwnerActionBar(Component.translatable(
                        "ui.apprenticecodex.search_beacon.entity.target_by_item",
                        heldStack.getDisplayName(),
                        Component.literal(SearchBeaconTargetManager.createDisplayLabel(definition))
                ).withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.CONSUME;
        }

        if (phase == Phase.ARMED) {
            if (player.isShiftKeyDown()) {
                returnOfferedItemToOwner(owner);
                return InteractionResult.CONSUME;
            }
            startSearch((ServerLevel) level());
            return InteractionResult.CONSUME;
        }

        if (phase == Phase.RESULT) {
            finishAndDiscard();
            return InteractionResult.CONSUME;
        }

        return InteractionResult.CONSUME;
    }

    private void sendOwnerActionBar(Component message) {
        var owner = getOwner();
        if (owner != null) {
            owner.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    public void push(@NotNull Entity entity) {
    }

    @Override
    protected void doPush(@NotNull Entity entity) {
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource damageSource) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag compoundTag) {
    }

    @Override
    public void addAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag compoundTag) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this,
                "main",
                0,
                state -> {
                    state.setAnimation(ANIM_IDLE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private enum Phase {
        IDLE,
        ARMED,
        SEARCHING,
        RESULT
    }

    private record ParticleColors(float red, float green, float blue) {
    }
}
