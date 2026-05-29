package jp.aquafactory.apprenticecodex.spell.healingbloom;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.config.DamageMultiplierKey;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.HealingBloomPulsePacket;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.UndeadTools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class HealingBloomEntity extends PathfinderMob implements GeoEntity, AntiMagicSusceptible {
    public static final float WIDTH = 0.85f;
    public static final float HEIGHT = 1.8f;
    private static final int ACTIVATION_DELAY_TICK = 40;
    private static final int GROW_ANIMATION_TICK = 36;
    private static final int REGEN_APPLY_INTERVAL_TICK = 200;
    private static final int REGEN_DURATION_TICK = 300;
    private static final int UNDEAD_DAMAGE_INTERVAL_TICK = 50;
    private static final float UNDEAD_DAMAGE_AMOUNT = 4.0f;
    private static final int LIGHT_RETRY_INTERVAL_TICK = 20;
    private static final int PULSE_INTERVAL_TICK = 100;
    private static final int HEAL_INTERVAL_TICK = 80;
    private static final float PULSE_RADIUS_MARGIN = 0.25f;
    private static final int DEFAULT_FRUIT_GROWTH_INTERVAL_TICK = 20 * 60 * 3;
    private static final RawAnimation ANIM_GROW = RawAnimation.begin().thenPlay("grow");
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");

    private static final EntityDataAccessor<Integer> ANIMATION_STATE =
            SynchedEntityData.defineId(HealingBloomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIMATION_SERIAL =
            SynchedEntityData.defineId(HealingBloomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FRUIT_COUNT =
            SynchedEntityData.defineId(HealingBloomEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID ownerUuid;
    private @Nullable LivingEntity cachedOwner;
    private BlockPos anchorPos = BlockPos.ZERO;
    private int effectRange = 1;
    private int fruitGrowthIntervalTick = DEFAULT_FRUIT_GROWTH_INTERVAL_TICK;
    private int bloomAgeTick;
    private int activeTickCounter;
    private int fruitGrowthProgressTick;
    private int clientAnimationSerial;
    private boolean deathHandled;

    public HealingBloomEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public static AABB makePlacementAabb(Vec3 center) {
        var halfWidth = WIDTH / 2.0;
        return new AABB(
                center.x - halfWidth,
                center.y,
                center.z - halfWidth,
                center.x + halfWidth,
                center.y + HEIGHT,
                center.z + halfWidth
        );
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIMATION_STATE, BloomAnimationState.GROW.id);
        builder.define(ANIMATION_SERIAL, 1);
        builder.define(FRUIT_COUNT, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount == 2 && bloomAgeTick < ACTIVATION_DELAY_TICK) {
                EffectTools.createRingParticle(
                        position().add(0.0, 0.1, 0.0),
                        new Vec3(0.0, 1.0, 0.0),
                        0.35f,
                        12,
                        0.015f,
                        0.01,
                        ParticleTypes.END_ROD,
                        level()
                );
            }
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            tickOnServer(serverLevel);
        }
    }

    private void tickOnServer(ServerLevel level) {
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        var anchorCenter = getAnchorCenter();
        if (position().distanceToSqr(anchorCenter) > 0.0001) {
            setPos(anchorCenter.x, anchorCenter.y, anchorCenter.z);
        }
        if (shouldDiscardBecauseUnmanaged(level)) {
            discardWithoutDeath();
            return;
        }
        if (!canStayAtAnchor()) {
            dieFromAnchorLoss(level);
            return;
        }
        if (tickCount % HEAL_INTERVAL_TICK == 0 && isAlive() && getHealth() < getMaxHealth()) {
            heal(1.0f);
        }

        tryPlayGrowthSound(level);
        if (bloomAgeTick >= GROW_ANIMATION_TICK && getAnimationState() == BloomAnimationState.GROW) {
            setAnimationState(BloomAnimationState.IDLE, false);
        }

        ++bloomAgeTick;
        tickFruitGrowth();
        if (bloomAgeTick < ACTIVATION_DELAY_TICK) {
            return;
        }

        ++activeTickCounter;
        if (activeTickCounter == 1 || activeTickCounter % LIGHT_RETRY_INTERVAL_TICK == 0) {
            tryPlaceLight();
        }
        if (activeTickCounter == 1 || (activeTickCounter - 1) % REGEN_APPLY_INTERVAL_TICK == 0) {
            applyRegenerationAura(level);
        }
        if (activeTickCounter % UNDEAD_DAMAGE_INTERVAL_TICK == 0) {
            damageUndead(level);
        }
        if (activeTickCounter == 1 || activeTickCounter % PULSE_INTERVAL_TICK == 0) {
            emitPulse();
        }
    }

    private void tickFruitGrowth() {
        ++fruitGrowthProgressTick;
        if (fruitGrowthProgressTick < fruitGrowthIntervalTick) {
            return;
        }

        fruitGrowthProgressTick -= fruitGrowthIntervalTick;
        setFruitCount(getFruitCount() + 1);
    }

    private void applyRegenerationAura(ServerLevel level) {
        for (var target : level.getEntitiesOfClass(LivingEntity.class, createEffectArea(), LivingEntity::isAlive)) {
            if (target == this || UndeadTools.isUndead(target)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION_TICK, 0, false, true, true));
        }
    }

    private void damageUndead(ServerLevel level) {
        var owner = getOwner();
        for (var target : level.getEntitiesOfClass(LivingEntity.class, createEffectArea(), LivingEntity::isAlive)) {
            if (!UndeadTools.isUndead(target)) {
                continue;
            }

            var finalDamage = UNDEAD_DAMAGE_AMOUNT * ApprenticeCodexServerConfig.damageMultiplier(DamageMultiplierKey.HEALING_BLOOM);
            var source = owner != null
                    ? CombatTools.getDamageSource(level, this, owner, DamageTypes.HEALING_BLOOM)
                    : CombatTools.getDamageSource(level, this, DamageTypes.HEALING_BLOOM);
            CombatTools.applyDamage(target, finalDamage, source, SpellRegistry.HEALING_BLOOM.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
        }
    }

    private void emitPulse() {
        AudioTools.playSoundFromPosition(level(), position(), SoundRegistry.CLEANSE_CAST.get(), SoundSource.PLAYERS, 0.45f, 1.0f, 0.04f);
        Networks.sendToTrackingEntityAndSelf(
                this,
                new HealingBloomPulsePacket(
                        position().add(0.0, 0.05, 0.0),
                        effectRange + PULSE_RADIUS_MARGIN
                )
        );
    }

    private boolean hasLightBlock() {
        return level().getBlockState(getLightPos()).is(BlockRegistry.HEALING_BLOOM_LIGHT.get());
    }

    private void tryPlaceLight() {
        if (hasLightBlock()) {
            return;
        }

        var lightPos = getLightPos();
        if (!level().getBlockState(lightPos).canBeReplaced()) {
            return;
        }

        level().setBlockAndUpdate(lightPos, BlockRegistry.HEALING_BLOOM_LIGHT.get().defaultBlockState());
    }

    private void removeLightBlock(Level level) {
        var lightPos = getLightPos();
        if (level.getBlockState(lightPos).is(BlockRegistry.HEALING_BLOOM_LIGHT.get())) {
            level.removeBlock(lightPos, false);
        }
    }

    private AABB createEffectArea() {
        return getBoundingBox().inflate(effectRange);
    }

    public void setOwner(LivingEntity owner) {
        ownerUuid = owner.getUUID();
        cachedOwner = owner;
    }

    public @Nullable LivingEntity getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }
        if (ownerUuid != null && level() instanceof ServerLevel serverLevel) {
            var player = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
            if (player != null) {
                cachedOwner = player;
                return player;
            }
            var entity = serverLevel.getEntity(ownerUuid);
            if (entity instanceof LivingEntity livingEntity) {
                cachedOwner = livingEntity;
                return livingEntity;
            }
        }
        return null;
    }

    public @Nullable UUID getOwnerUuid() {
        return ownerUuid;
    }

    public @Nullable String getOwnerName() {
        var owner = getOwner();
        return owner != null ? owner.getName().getString() : null;
    }

    public void setAnchorPos(BlockPos anchorPos) {
        this.anchorPos = anchorPos.immutable();
    }

    public void setEffectRange(int effectRange) {
        this.effectRange = Math.max(1, effectRange);
    }

    public void setBloomMaxHealth(float maxHealth) {
        var attribute = getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(maxHealth);
        }
        setHealth(maxHealth);
    }

    public void setFruitGrowthInterval(int fruitGrowthIntervalTick) {
        this.fruitGrowthIntervalTick = Math.max(1, fruitGrowthIntervalTick);
    }

    public int getFruitCount() {
        return entityData.get(FRUIT_COUNT);
    }

    public int getRemainingTicksUntilNextFruit() {
        return Math.max(0, fruitGrowthIntervalTick - fruitGrowthProgressTick);
    }

    boolean managesLightAt(BlockPos pos) {
        return getLightPos().equals(pos);
    }

    private void setFruitCount(int fruitCount) {
        entityData.set(FRUIT_COUNT, Math.max(0, fruitCount));
    }

    private Vec3 getAnchorCenter() {
        return new Vec3(anchorPos.getX() + 0.5, getAnchorY(), anchorPos.getZ() + 0.5);
    }

    private BlockPos getLightPos() {
        return anchorPos.above();
    }

    private BloomAnimationState getAnimationState() {
        return BloomAnimationState.of(entityData.get(ANIMATION_STATE));
    }

    private void setAnimationState(BloomAnimationState state, boolean replay) {
        entityData.set(ANIMATION_STATE, state.id);
        if (replay) {
            entityData.set(ANIMATION_SERIAL, entityData.get(ANIMATION_SERIAL) + 1);
        }
    }

    private void dieFromAnchorLoss(ServerLevel level) {
        if (isDeadOrDying()) {
            return;
        }

        // 根本が失われた場合も消滅ではなく死亡処理へ寄せ、果実化やドロップの共通処理を通す。
        setHealth(0.0f);
        die(level.damageSources().genericKill());
    }

    public void dieFromReplacement() {
        if (level() instanceof ServerLevel serverLevel) {
            dieFromAnchorLoss(serverLevel);
        }
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        if (level().isClientSide || isRemoved() || isDeadOrDying()) {
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            var owner = getOwner();
            if (owner instanceof ServerPlayer serverPlayer) {
                HealingBloomManager.onBloomRemoved(serverPlayer, this);
            }
            setHealth(0.0f);
            die(serverLevel.damageSources().genericKill());
        }
    }

    public void discardWithoutDeath() {
        discard();
    }

    @Override
    public void die(@NotNull DamageSource damageSource) {
        if (!level().isClientSide && level() instanceof ServerLevel) {
            handleDeathDrops();
        }
        super.die(damageSource);
    }

    private void handleDeathDrops() {
        if (deathHandled) {
            return;
        }

        deathHandled = true;
        dropComfortBerries(getFruitCount());
        setFruitCount(0);
    }

    private void dropComfortBerries(int count) {
        var remaining = count;
        var maxStackSize = new ItemStack(ItemRegistry.COMFORT_BERRIES.get()).getMaxStackSize();
        while (remaining > 0) {
            var dropCount = Math.min(remaining, maxStackSize);
            spawnAtLocation(new ItemStack(ItemRegistry.COMFORT_BERRIES.get(), dropCount));
            remaining -= dropCount;
        }
    }

    private void giveComfortBerriesTo(Player player, int count) {
        var remaining = count;
        var maxStackSize = new ItemStack(ItemRegistry.COMFORT_BERRIES.get()).getMaxStackSize();
        while (remaining > 0) {
            var giveCount = Math.min(remaining, maxStackSize);
            var stack = new ItemStack(ItemRegistry.COMFORT_BERRIES.get(), giveCount);
            if (!player.addItem(stack)) {
                spawnAtLocation(stack);
            }
            remaining -= giveCount;
        }
    }

    private void tryPlayGrowthSound(ServerLevel level) {
        if (bloomAgeTick != 4 && bloomAgeTick != 14 && bloomAgeTick != 24 && bloomAgeTick != 34) {
            return;
        }

        AudioTools.playSoundFromPosition(level, position(), SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.75f, 0.95f, 0.08f);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)
                || source.is(net.minecraft.world.damagesource.DamageTypes.DROWN)
                || source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide) {
            removeLightBlock(level());
            if (reason.shouldDestroy()) {
                var owner = getOwner();
                if (owner instanceof ServerPlayer serverPlayer) {
                    HealingBloomManager.onBloomRemoved(serverPlayer, this);
                }
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldBeSaved() {
        return true;
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
    protected void doPush(@NotNull Entity entity) {
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, @NotNull DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean isAlliedTo(@NotNull Entity entity) {
        var owner = getOwner();
        if (entity == this || entity == owner) {
            return true;
        }
        return owner != null && owner.isAlliedTo(entity);
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

        if (getFruitCount() <= 0) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.apprenticecodex.healing_bloom.no_fruit").withStyle(ChatFormatting.RED)
                ));
            }
            return InteractionResult.CONSUME;
        }

        var harvestedFruit = getFruitCount();
        setFruitCount(0);
        giveComfortBerriesTo(player, harvestedFruit);
        level().playSound(null, blockPosition(), SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS,
                1.0f, 0.8f + level().random.nextFloat() * 0.4f);
        level().gameEvent(GameEvent.ENTITY_INTERACT, position(), GameEvent.Context.of(player));
        return InteractionResult.CONSUME;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        ownerUuid = compoundTag.hasUUID("OwnerUUID") ? compoundTag.getUUID("OwnerUUID") : null;
        cachedOwner = null;
        if (compoundTag.contains("AnchorX")) {
            anchorPos = new BlockPos(compoundTag.getInt("AnchorX"), compoundTag.getInt("AnchorY"), compoundTag.getInt("AnchorZ"));
        }
        effectRange = Math.max(1, compoundTag.getInt("EffectRange"));
        fruitGrowthIntervalTick = Math.max(1, compoundTag.getInt("FruitGrowthIntervalTick"));
        bloomAgeTick = Math.max(0, compoundTag.getInt("BloomAgeTick"));
        activeTickCounter = Math.max(0, compoundTag.getInt("ActiveTickCounter"));
        fruitGrowthProgressTick = Math.max(0, compoundTag.getInt("FruitGrowthProgressTick"));
        setFruitCount(compoundTag.getInt("FruitCount"));
        setAnimationState(bloomAgeTick >= GROW_ANIMATION_TICK ? BloomAnimationState.IDLE : BloomAnimationState.GROW, false);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        if (ownerUuid != null) {
            compoundTag.putUUID("OwnerUUID", ownerUuid);
        }
        compoundTag.putInt("AnchorX", anchorPos.getX());
        compoundTag.putInt("AnchorY", anchorPos.getY());
        compoundTag.putInt("AnchorZ", anchorPos.getZ());
        compoundTag.putInt("EffectRange", effectRange);
        compoundTag.putInt("FruitGrowthIntervalTick", fruitGrowthIntervalTick);
        compoundTag.putInt("BloomAgeTick", bloomAgeTick);
        compoundTag.putInt("ActiveTickCounter", activeTickCounter);
        compoundTag.putInt("FruitGrowthProgressTick", fruitGrowthProgressTick);
        compoundTag.putInt("FruitCount", getFruitCount());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this,
                "main",
                state -> {
                    if (level().isClientSide) {
                        var serial = entityData.get(ANIMATION_SERIAL);
                        if (serial != clientAnimationSerial) {
                            clientAnimationSerial = serial;
                            state.getController().forceAnimationReset();
                        }
                    }

                    switch (getAnimationState()) {
                        case GROW -> state.getController().setAnimation(ANIM_GROW);
                        case IDLE -> state.getController().setAnimation(ANIM_IDLE);
                    }
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private enum BloomAnimationState {
        GROW(0),
        IDLE(1);

        private final int id;

        BloomAnimationState(int id) {
            this.id = id;
        }

        private static BloomAnimationState of(int rawId) {
            return rawId == 1 ? IDLE : GROW;
        }
    }

    private boolean shouldDiscardBecauseUnmanaged(ServerLevel level) {
        if (ownerUuid == null) {
            return false;
        }

        var owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
        return owner != null && !HealingBloomManager.shouldKeepLoadedBloom(owner, this);
    }

    private boolean canStayAtAnchor() {
        return HealingBloomPlacementHelper.hasSupportBelow(level(), anchorPos);
    }

    private double getAnchorY() {
        return HealingBloomPlacementHelper.getSupportTopY(level(), anchorPos);
    }
}
