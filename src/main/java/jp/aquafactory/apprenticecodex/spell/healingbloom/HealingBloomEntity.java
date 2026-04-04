package jp.aquafactory.apprenticecodex.spell.healingbloom;

import io.redspace.ironsspellbooks.registries.SoundRegistry;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class HealingBloomEntity extends PathfinderMob implements GeoEntity {
    public static final float WIDTH = 0.85f;
    public static final float HEIGHT = 1.8f;
    private static final int ACTIVATION_DELAY_TICK = 40;
    private static final int GROW_ANIMATION_TICK = 36;
    private static final int WITHER_ANIMATION_TICK = 30;
    private static final int REGEN_APPLY_INTERVAL_TICK = 200;
    private static final int REGEN_DURATION_TICK = 300;
    private static final int UNDEAD_DAMAGE_INTERVAL_TICK = 50;
    private static final int LIGHT_RETRY_INTERVAL_TICK = 20;
    private static final int PULSE_INTERVAL_TICK = 100;
    private static final float PULSE_RADIUS_MARGIN = 0.25f;
    private static final float LIGHT_COLOR_YELLOW_RED = 1.0f;
    private static final float LIGHT_COLOR_YELLOW_GREEN = 0.82f;
    private static final float LIGHT_COLOR_YELLOW_BLUE = 0.24f;
    private static final float LIGHT_COLOR_RED_RED = 1.0f;
    private static final float LIGHT_COLOR_RED_GREEN = 0.22f;
    private static final float LIGHT_COLOR_RED_BLUE = 0.12f;
    private static final RawAnimation ANIM_GROW = RawAnimation.begin().thenPlay("grow");
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_WITHER = RawAnimation.begin().thenPlay("wither");

    private static final EntityDataAccessor<Integer> ANIMATION_STATE =
            SynchedEntityData.defineId(HealingBloomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIMATION_SERIAL =
            SynchedEntityData.defineId(HealingBloomEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LIGHT_COLOR_RED =
            SynchedEntityData.defineId(HealingBloomEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LIGHT_COLOR_GREEN =
            SynchedEntityData.defineId(HealingBloomEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LIGHT_COLOR_BLUE =
            SynchedEntityData.defineId(HealingBloomEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable UUID ownerUuid;
    private @Nullable LivingEntity cachedOwner;
    private BlockPos anchorPos = BlockPos.ZERO;
    private int effectRange = 1;
    private int witherTime = 20 * 60;
    private boolean naturalWithering;
    private boolean lightInitialized;
    private int witherAnimationTick;
    private int clientAnimationSerial;

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
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANIMATION_STATE, BloomAnimationState.GROW.id);
        entityData.define(ANIMATION_SERIAL, 1);
        entityData.define(LIGHT_COLOR_RED, LIGHT_COLOR_YELLOW_RED);
        entityData.define(LIGHT_COLOR_GREEN, LIGHT_COLOR_YELLOW_GREEN);
        entityData.define(LIGHT_COLOR_BLUE, LIGHT_COLOR_YELLOW_BLUE);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount == 2) {
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
        var owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }

        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        var anchorCenter = getAnchorCenter();
        if (position().distanceToSqr(anchorCenter) > 0.0001) {
            setPos(anchorCenter.x, anchorCenter.y, anchorCenter.z);
        }
        syncLightColor();
        tryPlayGrowthSound(level);

        if (tickCount >= GROW_ANIMATION_TICK && getAnimationState() == BloomAnimationState.GROW && !naturalWithering) {
            setAnimationState(BloomAnimationState.IDLE, false);
        }

        if (naturalWithering) {
            tickWitherAnimation(level);
            return;
        }

        if (tickCount >= witherTime) {
            startNaturalWither(level);
            tickWitherAnimation(level);
            return;
        }

        if (tickCount < ACTIVATION_DELAY_TICK) {
            return;
        }

        if (!lightInitialized) {
            tryPlaceLight();
            lightInitialized = true;
        }

        if (tickCount % REGEN_APPLY_INTERVAL_TICK == 0) {
            applyRegenerationAura(level);
        }
        if (tickCount % UNDEAD_DAMAGE_INTERVAL_TICK == 0) {
            damageUndead(level);
        }
        if (tickCount % PULSE_INTERVAL_TICK == 0) {
            emitPulse();
        }
        if (tickCount % LIGHT_RETRY_INTERVAL_TICK == 0 && !hasLightBlock()) {
            super.hurt(level.damageSources().magic(), 10.0f);
            if (isAlive()) {
                tryPlaceLight();
            }
        }
    }

    private void applyRegenerationAura(ServerLevel level) {
        for (var target : level.getEntitiesOfClass(LivingEntity.class, createEffectArea(), LivingEntity::isAlive)) {
            if (UndeadTools.isUndead(target)) {
                continue;
            }
            var showParticles = target != this;
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION_TICK, 0, false, showParticles, showParticles));
        }
    }

    private void damageUndead(ServerLevel level) {
        var owner = getOwner();
        for (var target : level.getEntitiesOfClass(LivingEntity.class, createEffectArea(), LivingEntity::isAlive)) {
            if (!UndeadTools.isUndead(target)) {
                continue;
            }

            var source = owner != null
                    ? CombatTools.getDamageSource(level, this, owner, DamageTypes.HEALING_BLOOM)
                    : CombatTools.getDamageSource(level, this, DamageTypes.HEALING_BLOOM);
            CombatTools.applyDamage(target, 4.0f, source, SpellRegistry.HEALING_BLOOM.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK);
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

    private void tickWitherAnimation(ServerLevel level) {
        if (witherAnimationTick == 20 || witherAnimationTick == 10) {
            playWitherLeafBreak(level);
        }
        if (witherAnimationTick > 0) {
            --witherAnimationTick;
        }
        if (witherAnimationTick > 0) {
            return;
        }

        removeLightBlock(level);
        spawnAtLocation(new ItemStack(ItemRegistry.COMFORT_BERRIES.get(), 3 + random.nextInt(2)));
        discard();
    }

    private void startNaturalWither(ServerLevel level) {
        naturalWithering = true;
        witherAnimationTick = WITHER_ANIMATION_TICK;
        setAnimationState(BloomAnimationState.WITHER, true);
        playWitherLeafBreak(level);
        removeLightBlock(level);
    }

    private boolean hasLightBlock() {
        return level().getBlockState(getLightPos()).is(BlockRegistry.HEALING_BLOOM_LIGHT.get());
    }

    private void tryPlaceLight() {
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

    public void setWitherTime(int witherTime) {
        this.witherTime = Math.max(ACTIVATION_DELAY_TICK + 1, witherTime);
    }

    public float getLifetimeProgress() {
        if (witherTime <= 0) {
            return 1.0f;
        }
        return Math.min(1.0f, tickCount / (float) witherTime);
    }

    public int getRemainingWitherTicks() {
        return Math.max(0, witherTime - tickCount);
    }

    public float getLightColorRed() {
        return entityData.get(LIGHT_COLOR_RED);
    }

    public float getLightColorGreen() {
        return entityData.get(LIGHT_COLOR_GREEN);
    }

    public float getLightColorBlue() {
        return entityData.get(LIGHT_COLOR_BLUE);
    }

    boolean managesLightAt(BlockPos pos) {
        return getLightPos().equals(pos);
    }

    private Vec3 getAnchorCenter() {
        return new Vec3(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5);
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

    private void syncLightColor() {
        // 寿命はサーバー側だけが正値を持つため、光源色も花から同期して client renderer に渡す。
        var progress = getLifetimeProgress();
        entityData.set(LIGHT_COLOR_RED, net.minecraft.util.Mth.lerp(progress, LIGHT_COLOR_YELLOW_RED, LIGHT_COLOR_RED_RED));
        entityData.set(LIGHT_COLOR_GREEN, net.minecraft.util.Mth.lerp(progress, LIGHT_COLOR_YELLOW_GREEN, LIGHT_COLOR_RED_GREEN));
        entityData.set(LIGHT_COLOR_BLUE, net.minecraft.util.Mth.lerp(progress, LIGHT_COLOR_YELLOW_BLUE, LIGHT_COLOR_RED_BLUE));
    }

    private void tryPlayGrowthSound(ServerLevel level) {
        if (tickCount != 4 && tickCount != 14 && tickCount != 24 && tickCount != 34) {
            return;
        }

        AudioTools.playSoundFromPosition(level, position(), SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.75f, 0.95f, 0.08f);
    }

    private void playWitherLeafBreak(ServerLevel level) {
        AudioTools.playSoundFromPosition(level, position(), SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 0.55f, 0.85f, 0.10f);
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
        }
        super.remove(reason);
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

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable(
                            "ui.apprenticecodex.bloom_wither_time",
                            io.redspace.ironsspellbooks.api.util.Utils.timeFromTicks(getRemainingWitherTicks(), 1)
                    ).withStyle(ChatFormatting.YELLOW)
            ));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
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
                        case WITHER -> state.getController().setAnimation(ANIM_WITHER);
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
        IDLE(1),
        WITHER(2);

        private final int id;

        BloomAnimationState(int id) {
            this.id = id;
        }

        private static BloomAnimationState of(int rawId) {
            return switch (rawId) {
                case 1 -> IDLE;
                case 2 -> WITHER;
                default -> GROW;
            };
        }
    }
}
