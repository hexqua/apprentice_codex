package jp.aquafactory.apprenticecodex.spell.searchbeacon;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.SearchBeaconState;
import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import java.util.Objects;
import java.util.UUID;

public class SearchBeaconEntity extends PathfinderMob implements GeoEntity {
    public static final float WIDTH = 0.7f;
    public static final float HEIGHT = 1.45f;
    private static final int SEARCH_RESOLVE_TICKS = 20 * 3;
    private static final int INITIAL_HINT_DELAY_TICKS = 20;
    private static final int OFFER_CHECK_INTERVAL_TICKS = 4;
    private static final int DIRECTION_PARTICLE_INTERVAL_TICKS = 8;
    private static final int REOFFER_COOLDOWN_TICKS = 20 * 5;
    private static final int SUMMON_PARTICLE_TICKS = 3;
    private static final double OFFER_PICKUP_RANGE = 0.85;
    private static final double MIN_HORIZONTAL_DISTANCE = 0.1;
    private static final double ALL_PHASE_DISCARD_DISTANCE_SQR = 32.0 * 32.0;
    private static final double RETURN_THROW_SPEED = 0.42;
    private static final double RETURN_THROW_UPWARD = 0.14;
    private static final double RETURN_TARGET_HEIGHT = 0.7;
    private static final double PARTICLE_BASE_Y = 9.0 / 16.0;
    private static final float SUMMON_END_ROD_SPEED = 0.025f;
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final float DIRECTION_RHOMBUS_SIZE = 0.28f;
    private static final float DIRECTION_SPARK_SIZE = 0.11f;
    private static final float FLAME_CIRCLE_SIZE = 0.24f;
    private static final float FLAME_SPARK_SIZE = 0.12f;
    private static final float UNKNOWN_RED = 0.95f;
    private static final float UNKNOWN_GREEN = 0.80f;
    private static final float UNKNOWN_BLUE = 0.22f;
    private static final float SEARCHED_RED = 0.32f;
    private static final float SEARCHED_GREEN = 0.66f;
    private static final float SEARCHED_BLUE = 1.0f;
    private static final float TRAVERSED_RED = 1.0f;
    private static final float TRAVERSED_GREEN = 0.28f;
    private static final float TRAVERSED_BLUE = 0.22f;
    private static final float FLAME_RED = 0.26f;
    private static final float FLAME_GREEN = 0.92f;
    private static final float FLAME_BLUE = 0.38f;

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
    private boolean refundIssued;

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
            tickClientEffects();
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

        if (owner.distanceToSqr(this) > ALL_PHASE_DISCARD_DISTANCE_SQR) {
            if (phase == Phase.RESULT) {
                finishAndDiscard();
            } else {
                discard();
            }
            return;
        }

        switch (phase) {
            case IDLE -> tickIdle(serverLevel);
            case ARMED -> tickArmed(serverLevel);
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
        if (phaseTicks == INITIAL_HINT_DELAY_TICKS) {
            sendRandomEmptyHint();
        }
        absorbNearbyOfferedItem(level);
    }

    private void tickArmed(ServerLevel level) {
        absorbNearbyOfferedItem(level);
    }

    private void absorbNearbyOfferedItem(ServerLevel level) {
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
            emitDirectionParticles(level, searchResult, resolveSpellState());
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

        if (!offeredItem.isEmpty() && !ItemStack.isSameItemSameTags(offeredStack, offeredItem)) {
            return;
        }

        var acceptedCount = getAcceptedItemCount(offeredStack);
        if (acceptedCount <= 0) {
            return;
        }

        if (offeredItem.isEmpty()) {
            offeredItem = offeredStack.copyWithCount(acceptedCount);
        } else {
            offeredItem.grow(acceptedCount);
        }
        targetLabel = SearchBeaconTargetManager.createDisplayLabel(definition);

        var remainingCount = offeredStack.getCount() - acceptedCount;
        if (remainingCount > 0) {
            itemEntity.setItem(offeredStack.copyWithCount(remainingCount));
        } else {
            itemEntity.discard();
        }

        searchRange = getSearchRangeForCount(offeredItem.getCount());
        searchSession = null;
        searchResult = null;
        sendOwnerActionBar(Component.translatable(
                "ui.apprenticecodex.search_beacon.entity.current_range",
                offeredItem.getDisplayName(),
                searchRange
        ).withStyle(ChatFormatting.YELLOW));
        playOfferEffects();
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
            refundIssued = false;
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
        level.playSound(null, blockPosition(), SoundRegistry.VANILLA_START_SEARCH.get(), SoundSource.BLOCKS, 0.8f, 1.15f);
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
                if (state.markSearched(result.foundStructureMarkers())) {
                    data.markDirty(CodexSpellStateTypeRegister.SEARCH_BEACON_STATE.id());
                }
            });
        }

        if (SearchBeaconSearchService.shouldRefundOfferedItems(result)) {
            issueRefundIfNeeded();
        }
        transitionTo(Phase.RESULT);
    }

    private void emitDirectionParticles(ServerLevel level, SearchBeaconSearchService.SearchResult result, SearchBeaconState state) {
        var base = getParticleBasePosition();
        for (var located : result.locatedStructures()) {
            var dx = located.center().getX() + 0.5 - base.x;
            var dz = located.center().getZ() + 0.5 - base.z;
            var horizontalLength = Math.sqrt(dx * dx + dz * dz);
            if (horizontalLength < MIN_HORIZONTAL_DISTANCE) {
                continue;
            }

            var dirX = dx / horizontalLength;
            var dirZ = dz / horizontalLength;
            var colors = getParticleColors(resolveDisplayKnowledge(located, state));
            var tipDistance = 2.6;
            var tipX = base.x + dirX * tipDistance;
            var tipY = base.y + 0.16;
            var tipZ = base.z + dirZ * tipDistance;
            level.sendParticles(
                    new AdditiveGlowParticleOptions(
                            ParticleRegistry.ADDITIVE_RHOMBUS.get(),
                            DIRECTION_RHOMBUS_SIZE,
                            colors.red(),
                            colors.green(),
                            colors.blue(),
                            4,
                            12,
                            3,
                            0.95f,
                            1.10f,
                            0.80f,
                            1.0f,
                            0.08f,
                            0.72f,
                            0.76f,
                            false
                    ),
                    tipX, tipY, tipZ,
                    1,
                    dirX * 0.01,
                    0.02,
                    dirZ * 0.01,
                    0.0
            );

            for (int i = 0; i < 7; i++) {
                var t = (i + 1) / 8.0;
                var x = Mth.lerp(t, base.x, tipX);
                var y = Mth.lerp(t, base.y + 0.06, tipY);
                var z = Mth.lerp(t, base.z, tipZ);
                level.sendParticles(
                        new AdditiveGlowParticleOptions(
                                ParticleRegistry.ADDITIVE_SPARK.get(),
                                DIRECTION_SPARK_SIZE,
                                colors.red(),
                                colors.green(),
                                colors.blue(),
                                1,
                                10,
                                2,
                                0.95f,
                                1.18f,
                                0.85f,
                                1.0f,
                                0.04f,
                                0.72f,
                                0.78f,
                                false
                        ),
                        x, y, z,
                        1,
                        dirX * 0.015,
                        0.008,
                        dirZ * 0.015,
                        0.0
                );
            }
        }
    }

    private static SearchBeaconState.StructureKnowledge resolveDisplayKnowledge(
            SearchBeaconSearchService.LocatedStructure located,
            SearchBeaconState state
    ) {
        var currentKnowledge = state.getKnowledge(located.marker());
        if (currentKnowledge == SearchBeaconState.StructureKnowledge.TRAVERSED) {
            return SearchBeaconState.StructureKnowledge.TRAVERSED;
        }
        return located.knowledge();
    }

    private void finishAndDiscard() {
        var shouldRefund = SearchBeaconSearchService.shouldRefundOfferedItems(searchResult);
        if (shouldRefund && !refundIssued) {
            issueRefundIfNeeded();
        }
        clearOfferState();
        searchSession = null;
        discard();
    }

    private void returnOfferedItemToOwner(ServerPlayer owner) {
        if (offeredItem.isEmpty()) {
            return;
        }

        throwItemTowardOwner(owner, offeredItem.copy());
        clearOfferState();
        transitionTo(Phase.IDLE);
    }

    private void clearOfferState() {
        offeredItem = ItemStack.EMPTY;
        targetLabel = "";
        searchRange = 0;
        searchResult = null;
        searchSession = null;
        refundIssued = false;
    }

    private int getAcceptedItemCount(ItemStack incomingStack) {
        var offeredCount = incomingStack.getCount();
        if (offeredCount <= 0) {
            return 0;
        }

        var remainingCount = getMaxAcceptedItemCount() - offeredItem.getCount();
        if (remainingCount <= 0) {
            return 0;
        }

        return Math.min(offeredCount, remainingCount);
    }

    private int getMaxAcceptedItemCount() {
        var maxSearchRange = SearchBeaconSearchService.getMaxSearchRange();
        if (maxSearchRange <= 0 || initialRange <= 0) {
            return 0;
        }
        if (additionalRangePerItem <= 0 || initialRange >= maxSearchRange) {
            return 1;
        }

        var remainingRange = Math.max(0, maxSearchRange - initialRange);
        var maxExtraItems = Mth.positiveCeilDiv(remainingRange, additionalRangePerItem);
        return 1 + maxExtraItems;
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

    boolean isOwnedBy(UUID ownerId) {
        return Objects.equals(ownerUuid, ownerId);
    }

    public @Nullable String getOwnerName() {
        var owner = getOwner();
        return owner != null ? owner.getName().getString() : null;
    }

    public ItemStack getOfferedItem() {
        return offeredItem.copy();
    }

    public @Nullable String getTargetLabel() {
        return targetLabel.isBlank() ? null : targetLabel;
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
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        var owner = getOwner();
        if (player != owner) {
            return InteractionResult.PASS;
        }

        if (phase == Phase.IDLE) {
            if (player.isShiftKeyDown() && offeredItem.isEmpty()) {
                resetOwnerCooldown(owner);
                discard();
                return InteractionResult.CONSUME;
            }

            var heldStack = player.getItemInHand(hand);
            if (heldStack.isEmpty()) {
                return sendRandomEmptyHint() ? InteractionResult.CONSUME : InteractionResult.PASS;
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

    private boolean sendRandomEmptyHint() {
        var candidate = SearchBeaconTargetManager.getRandomHintCandidate(random);
        if (candidate == null) {
            return false;
        }

        sendOwnerActionBar(Component.translatable(
                "ui.apprenticecodex.search_beacon.entity.hint_with_empty",
                candidate.itemStack().getDisplayName(),
                Component.literal(candidate.targetLabel())
        ).withStyle(ChatFormatting.YELLOW));
        return true;
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
    public void onClientRemoval() {
        emitSummonParticlesBurst();
        super.onClientRemoval();
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

    private void tickClientEffects() {
        emitSummonParticles();
        emitFlameParticles();
    }

    private void emitSummonParticles() {
        if (tickCount < 1 || tickCount > SUMMON_PARTICLE_TICKS) {
            return;
        }

        emitSummonParticlesBurst();
    }

    private void emitSummonParticlesBurst() {
        for (int i = 0; i < 10; i++) {
            level().addParticle(
                    ParticleTypes.END_ROD,
                    getX() + (random.nextDouble() - 0.5) * WIDTH,
                    getY() + random.nextDouble() * HEIGHT,
                    getZ() + (random.nextDouble() - 0.5) * WIDTH,
                    (random.nextDouble() - 0.5) * SUMMON_END_ROD_SPEED,
                    (random.nextDouble() - 0.5) * SUMMON_END_ROD_SPEED + 0.01,
                    (random.nextDouble() - 0.5) * SUMMON_END_ROD_SPEED
            );
        }
    }

    private void emitFlameParticles() {
        var base = getParticleBasePosition();
        if ((tickCount + getId()) % 3 == 0) {
            level().addParticle(
                    new AdditiveGlowParticleOptions(
                            ParticleRegistry.ADDITIVE_CIRCLE.get(),
                            FLAME_CIRCLE_SIZE,
                            FLAME_RED,
                            FLAME_GREEN,
                            FLAME_BLUE,
                            6,
                            16,
                            4,
                            0.85f,
                            1.12f,
                            0.45f,
                            0.82f,
                            0.08f,
                            0.72f,
                            0.78f,
                            false
                    ),
                    base.x,
                    base.y + 0.02,
                    base.z,
                    0.0,
                    0.004,
                    0.0
            );
        }

        var sparkCount = phase == Phase.RESULT ? 2 : 1;
        for (int i = 0; i < sparkCount; i++) {
            var angle = random.nextDouble() * Math.PI * 2.0;
            var radius = 0.04 + random.nextDouble() * 0.08;
            level().addParticle(
                    new AdditiveGlowParticleOptions(
                            ParticleRegistry.ADDITIVE_SPARK.get(),
                            FLAME_SPARK_SIZE,
                            FLAME_RED,
                            FLAME_GREEN,
                            FLAME_BLUE,
                            4,
                            12,
                            3,
                            0.90f,
                            1.15f,
                            0.85f,
                            1.0f,
                            0.04f,
                            0.70f,
                            0.74f,
                            false
                    ),
                    base.x + Math.cos(angle) * radius,
                    base.y + random.nextDouble() * 0.10,
                    base.z + Math.sin(angle) * radius,
                    (random.nextDouble() - 0.5) * 0.01,
                    0.02 + random.nextDouble() * 0.01,
                    (random.nextDouble() - 0.5) * 0.01
            );
        }
    }

    private Vec3 getParticleBasePosition() {
        return position().add(0.0, PARTICLE_BASE_Y, 0.0);
    }

    private void playOfferEffects() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var base = getParticleBasePosition();
        serverLevel.sendParticles(ParticleTypes.END_ROD, base.x, base.y + 0.08, base.z, 18, 0.18, 0.14, 0.18, 0.04);
        serverLevel.sendParticles(ParticleTypes.FIREWORK, base.x, base.y + 0.12, base.z, 12, 0.10, 0.10, 0.10, 0.08);
        serverLevel.playSound(null, blockPosition(), SoundRegistry.VANILLA_BRAZIER_SACRIFICE.get(), SoundSource.BLOCKS, 0.7f, 1.15f);
    }

    private void issueRefundIfNeeded() {
        var owner = getOwner();
        if (owner == null || offeredItem.isEmpty() || refundIssued) {
            return;
        }

        var refundCount = Mth.floor(offeredItem.getCount() / 2.0f);
        if (refundCount <= 0) {
            return;
        }

        throwItemTowardOwner(owner, offeredItem.copyWithCount(refundCount));
        if (level() instanceof ServerLevel serverLevel) {
            var base = getParticleBasePosition();
            serverLevel.sendParticles(ParticleTypes.END_ROD, base.x, base.y + 0.10, base.z, 10, 0.12, 0.10, 0.12, 0.03);
        }
        refundIssued = true;
    }

    private void throwItemTowardOwner(ServerPlayer owner, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        // 返却が見えづらいので、足元に落とすのではなく owner 側へ投げ返して気づきやすくする。
        var dropped = spawnAtLocation(stack);
        if (dropped == null) {
            return;
        }

        var direction = owner.position().add(0.0, RETURN_TARGET_HEIGHT, 0.0).subtract(position());
        if (direction.lengthSqr() > 1.0E-6) {
            direction = direction.normalize().scale(RETURN_THROW_SPEED);
        } else {
            direction = Vec3.ZERO;
        }
        dropped.setDeltaMovement(direction.x, direction.y + RETURN_THROW_UPWARD, direction.z);
        dropped.setPickUpDelay(0);
        ignoredOfferItemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        ignoredOfferUntilTick = tickCount + REOFFER_COOLDOWN_TICKS;
        hasImpulse = true;
    }

    private void resetOwnerCooldown(ServerPlayer owner) {
        var magicData = MagicData.getPlayerMagicData(owner);
        if (magicData.getPlayerCooldowns().removeCooldown(SpellRegistry.SEARCH_BEACON.get().getSpellId())) {
            magicData.getPlayerCooldowns().syncToPlayer(owner);
        }
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
