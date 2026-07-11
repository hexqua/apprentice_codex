package jp.aquafactory.apprenticecodex.spell.totemofpermafrost;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.TotemOfPermafrostPulsePacket;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.PlacementHelper;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.CombatOwnerUuidSource;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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

import java.util.Optional;
import java.util.UUID;

public class TotemOfPermafrostTotemEntity extends PathfinderMob implements GeoEntity, CombatOwnerUuidSource {
    public static final float WIDTH = 0.8f;
    public static final float HEIGHT = 0.9f;
    private static final int FIRST_PULSE_TICK = 10;
    private static final int PULSE_INTERVAL_TICK = 15;
    private static final int FREEZE_TICK_INCREMENT = 40;
    private static final int MAX_FREEZE_TICK = 300;
    private static final int SLOWNESS_DURATION_TICK = 40;
    private static final float PULSE_RADIUS_MARGIN = 0.5f;
    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlayAndHold("spawn");

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(TotemOfPermafrostTotemEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> PULSE_SEQUENCE =
            SynchedEntityData.defineId(TotemOfPermafrostTotemEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable LivingEntity cachedOwner;
    private BlockPos anchorPos = BlockPos.ZERO;
    private double radius = 3.0;
    private float damage;
    private int slownessAmplifier;
    private int clientPulseSequence;
    private int clientLastPulseTick = -1000;

    public TotemOfPermafrostTotemEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 0.0);
    }

    public static AABB makePlacementAabb(Vec3 center) {
        double halfWidth = WIDTH / 2.0;
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
        entityData.define(OWNER_UUID, Optional.empty());
        entityData.define(PULSE_SEQUENCE, 0);
    }

    @Override
    protected void registerGoals() {
        // do nothing.
    }

    @Override
    public void onClientRemoval() {
        emitSnowScatterParticles();
        super.onClientRemoval();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount == 2) {
                emitSnowScatterParticles();
            }
            var pulseSequence = entityData.get(PULSE_SEQUENCE);
            if (pulseSequence != clientPulseSequence) {
                clientPulseSequence = pulseSequence;
                clientLastPulseTick = tickCount;
            }
            return;
        }

        if (level() instanceof ServerLevel serverLevel) {
            tickOnServer(serverLevel);
        }
    }

    private void tickOnServer(ServerLevel level) {
        var owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != level) {
            discard();
            return;
        }

        if (PlacementHelper.hasSupportBelow(level(), anchorPos)) {
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);
            var anchorCenter = getAnchorCenter();
            if (position().distanceToSqr(anchorCenter) > 0.0001) {
                setPos(anchorCenter.x, anchorCenter.y, anchorCenter.z);
            }
        } else {
            setNoGravity(false);
        }

        if (tickCount >= FIRST_PULSE_TICK && (tickCount - FIRST_PULSE_TICK) % PULSE_INTERVAL_TICK == 0) {
            pulse(level, owner);
        }
    }

    private void pulse(ServerLevel level, LivingEntity owner) {
        AudioTools.playSoundFromPosition(
                level,
                position(),
                io.redspace.ironsspellbooks.registries.SoundRegistry.ICE_CAST.get(),
                SoundSource.PLAYERS,
                0.75f,
                1.0f,
                0.05f
        );
        Networks.sendToTrackingEntityAndSelf(
                this,
                new TotemOfPermafrostPulsePacket(position().add(0.0, 0.05, 0.0), (float) (radius + PULSE_RADIUS_MARGIN))
        );
        entityData.set(PULSE_SEQUENCE, entityData.get(PULSE_SEQUENCE) + 1);

        var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.TOTEM_OF_PERMAFROST);
        var effectArea = AABB.ofSize(position(), radius * 2.0 + 1.0, 3.0, radius * 2.0 + 1.0);
        for (var target : CombatTools.resolveUniqueCombatTargets(level.getEntities(this, effectArea,
                entity -> entity.isAlive() && CombatTools.isValidCombatTarget(entity, owner)))) {
            if (!RaycastTools.hasLineOfSight(level, this, target)) {
                continue;
            }
            if (CombatTools.applyDamage(target, damage, source, SpellRegistry.TOTEM_OF_PERMAFROST.get().getSchoolType(), CombatTools.KnockbackTypes.NO_KNOCKBACK)
                    && target instanceof LivingEntity livingTarget) {
                livingTarget.setTicksFrozen(Math.min(MAX_FREEZE_TICK, livingTarget.getTicksFrozen() + FREEZE_TICK_INCREMENT));
                livingTarget.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        SLOWNESS_DURATION_TICK,
                        slownessAmplifier,
                        false,
                        true,
                        true
                ));
            }
        }
    }

    public void setOwner(LivingEntity owner) {
        cachedOwner = owner;
        entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
    }

    public @Nullable LivingEntity getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }
        var ownerUuid = entityData.get(OWNER_UUID);
        if (ownerUuid.isPresent() && level() instanceof ServerLevel serverLevel) {
            var player = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid.get());
            if (player != null) {
                cachedOwner = player;
                return player;
            }
            var entity = serverLevel.getEntity(ownerUuid.get());
            if (entity instanceof LivingEntity livingEntity) {
                cachedOwner = livingEntity;
                return livingEntity;
            }
        }
        return null;
    }

    @Override
    public @Nullable UUID getCombatOwnerUuid() {
        return entityData.get(OWNER_UUID).orElse(null);
    }

    public void setAnchorPos(BlockPos anchorPos) {
        this.anchorPos = anchorPos.immutable();
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setSlownessAmplifier(int slownessAmplifier) {
        this.slownessAmplifier = Math.max(0, slownessAmplifier);
    }

    public void setRadius(double radius) {
        this.radius = Math.max(0.0, radius);
    }

    public float getIceGlowStrength(float partialTick) {
        if (clientLastPulseTick < 0) {
            return 0.0f;
        }

        var age = Math.max(0.0f, tickCount - clientLastPulseTick + partialTick);
        if (age >= 20.0f) {
            return 0.0f;
        }

        var progress = age / 20.0f;
        return (float) Math.cos(progress * Math.PI * 0.5);
    }

    private Vec3 getAnchorCenter() {
        return new Vec3(
                anchorPos.getX() + 0.5,
                PlacementHelper.getSupportTopY(level(), anchorPos),
                anchorPos.getZ() + 0.5
        );
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
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return false;
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
                    state.setAnimation(SPAWN);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private void emitSnowScatterParticles() {
        EffectTools.createRingParticle(
                position().add(0.0, 0.1, 0.0),
                new Vec3(0.0, 1.0, 0.0),
                0.35f,
                12,
                0.015f,
                0.01,
                ParticleTypes.SNOWFLAKE,
                level()
        );
    }
}
