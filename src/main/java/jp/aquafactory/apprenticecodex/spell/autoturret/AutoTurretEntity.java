package jp.aquafactory.apprenticecodex.spell.autoturret;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.archermultiple.ArcherMultipleBowEntity;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.UUID;

public class AutoTurretEntity extends PathfinderMob implements GeoEntity {
    public static final float WIDTH = 0.9f;
    public static final float HEIGHT = 1.6f;
    private static final int CHARGE_TICK = 15;
    private static final int COOLDOWN_TICK = 8;
    private static final int DELAY_FIRST_AUTO_LOCK_ON_SHOT_TICK = 10;
    private static final int KEEP_LOCK_ON_TICK_FOR_CHANGE_TARGET = 60;
    private static final int KEEP_LOCK_ON_TICK_IN_LOST_LOR = 20;
    private static final int KEEP_FIRE_CONTINUE_TICK = 40;
    private static final int DISCARD_DELAY_TICK = 10;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private static final EntityDataAccessor<Integer> CHARGE_STAGE =
            SynchedEntityData.defineId(AutoTurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HIT_SEQUENCE =
            SynchedEntityData.defineId(AutoTurretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HIT_POSITION_X =
            SynchedEntityData.defineId(AutoTurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HIT_POSITION_Y =
            SynchedEntityData.defineId(AutoTurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HIT_POSITION_Z =
            SynchedEntityData.defineId(AutoTurretEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_PITCH =
            SynchedEntityData.defineId(AutoTurretEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID ownerUuid;
    private LivingEntity cachedOwner;
    private BlockPos anchorPos = BlockPos.ZERO;
    private float damage;
    private int restBulletCount;
    private int currentChargeTick;
    private int currentCoolDownTick;
    private int currentLockOnTick;
    private int currentLostSightTick;
    private int keepFireContinueTick;
    private int currentHitSequence;
    private int discardDelayTick = -1;
    private Entity autoTarget;

    public AutoTurretEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
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
    protected void registerGoals() {
        super.registerGoals();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGE_STAGE, 0);
        builder.define(HIT_SEQUENCE, 0);
        builder.define(HIT_POSITION_X, 0.0f);
        builder.define(HIT_POSITION_Y, 0.0f);
        builder.define(HIT_POSITION_Z, 0.0f);
        builder.define(AIM_PITCH, 0.0f);
    }

    @Override
    public void onClientRemoval(){
        var level = level();
        EffectTools.createRingParticle(
                position(),
                new Vec3(0, 1, 0),
                0.4f,
                8,
                0.015f,
                0.01,
                ParticleTypes.END_ROD,
                level
        );
        super.onClientRemoval();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // super.tickが走るとfirstTickが使えないため少し遅らせる.
            if (tickCount == 2) {
                EffectTools.createRingParticle(
                        position().add(0.0, 0.2, 0.0),
                        new Vec3(0.0, 1.0, 0.0),
                        0.4f,
                        8,
                        0.015f,
                        0.01,
                        ParticleTypes.END_ROD,
                        level()
                );
            }

            var hitSequence = entityData.get(HIT_SEQUENCE);
            var crossbowHeight = 11.0f / 16.0f;
            if (currentHitSequence != hitSequence) {
                currentHitSequence = hitSequence;
                var hitPosition = new Vec3(entityData.get(HIT_POSITION_X), entityData.get(HIT_POSITION_Y), entityData.get(HIT_POSITION_Z));
                EffectTools.createLineParticle(position().add(0.0, crossbowHeight, 0.0), hitPosition, 0.5, 0.1, 0.1, ParticleTypes.CRIT, level());
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

        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        var anchorCenter = getAnchorCenter();
        if (position().distanceToSqr(anchorCenter) > 0.0001) {
            setPos(anchorCenter.x, anchorCenter.y, anchorCenter.z);
        }

        if (discardDelayTick >= 0) {
            if (--discardDelayTick <= 0) {
                discard();
            }
            return;
        }

        if (autoTarget != null && (autoTarget.isRemoved() || !autoTarget.isAlive())) {
            autoTarget = null;
        }

        if (autoTarget == null || currentLockOnTick >= KEEP_LOCK_ON_TICK_FOR_CHANGE_TARGET) {
            if (tickCount % 10 == 0) {
                var newTarget = searchAutoTarget(level);
                if (newTarget != null && newTarget != autoTarget) {
                    if (keepFireContinueTick == 0) {
                        currentCoolDownTick = DELAY_FIRST_AUTO_LOCK_ON_SHOT_TICK;
                    }
                    autoTarget = newTarget;
                    currentLockOnTick = 0;
                    currentLostSightTick = 0;
                    keepFireContinueTick = KEEP_FIRE_CONTINUE_TICK;
                }
            }
        }

        Entity target = autoTarget;
        if (target != null && !RaycastTools.hasLineOfSight(level, this, target)) {
            ++currentLostSightTick;
            if (currentLostSightTick >= KEEP_LOCK_ON_TICK_IN_LOST_LOR) {
                autoTarget = null;
                target = null;
            }
        } else {
            currentLostSightTick = 0;
        }

        if (target != null) {
            var targetPosition = target.position().add(0, target.getBbHeight() / 2, 0);
            var faceVector = targetPosition.subtract(position()).normalize();
            var yawPitch = RotationTools.calculateYawPitchByDirection(faceVector);
            setYRot(yawPitch.yaw());
            setYHeadRot(yawPitch.yaw());
            setYBodyRot(yawPitch.yaw());
            entityData.set(AIM_PITCH, yawPitch.pitch());
            currentLockOnTick++;
            keepFireContinueTick = KEEP_FIRE_CONTINUE_TICK;
        } else {
            setYHeadRot(getYRot());
            setYBodyRot(getYRot());
            entityData.set(AIM_PITCH, 0.0f);
            currentLockOnTick = 0;
            if (keepFireContinueTick > 0) {
                --keepFireContinueTick;
            }
        }

        if (currentCoolDownTick > 0) {
            --currentCoolDownTick;
        } else if (target != null && target.isAlive()) {
            if (currentChargeTick < CHARGE_TICK) {
                ++currentChargeTick;
                // 最大ステージになるには10～15.
                if (currentChargeTick == 10) {
                    playCrossbowLoadingEnd(level);
                }
            } else {
                fire(target, level, owner);
                currentChargeTick = 0;
                currentCoolDownTick = COOLDOWN_TICK;
                if (restBulletCount > 0) {
                    --restBulletCount;
                }
                if (restBulletCount <= 0) {
                    discardDelayTick = DISCARD_DELAY_TICK;
                }
            }
        }

        setStageByCurrentCharge();
    }

    private void playCrossbowLoadingEnd(ServerLevel level) {
        AudioTools.playSoundFromEntity(level, this, SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1.0f, 1.0f, 0.0f);
    }

    private void fire(Entity target, ServerLevel level, LivingEntity owner) {
        var targetPosition = RaycastTools.getEntityTargetPosition(target);
        var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.AUTO_TURRET);
        CombatTools.applyDamage(target, damage, source, SpellRegistry.AUTO_TURRET.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
        AudioTools.playSoundFromEntity(level, this, SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.5f);

        if (target instanceof Mob mob && !mob.isAlliedTo(this)) {
            mob.setTarget(this);
            mob.setLastHurtByMob(this);
        }

        var sequence = entityData.get(HIT_SEQUENCE);
        entityData.set(HIT_SEQUENCE, sequence + 1);
        entityData.set(HIT_POSITION_X, (float) targetPosition.x);
        entityData.set(HIT_POSITION_Y, (float) targetPosition.y);
        entityData.set(HIT_POSITION_Z, (float) targetPosition.z);
    }

    private Entity searchAutoTarget(Level level) {
        var owner = getOwner();
        if (owner == null) {
            return null;
        }

        var searchRange = getAttributeValue(Attributes.FOLLOW_RANGE);
        if (searchRange <= 0.0) {
            searchRange = 24.0;
        }

        var rangeSqr = searchRange * searchRange;
        var searchBox = getBoundingBox().inflate(searchRange);

        // 設置向きに依存せず全方位を拾うが、再探索自体は 10tick ごとなので負荷は増やしすぎない.
        return level.getEntities(this, searchBox, e ->
                        e.isAlive()
                                && e.distanceToSqr(this) <= rangeSqr
                                && CombatTools.isValidCombatTarget(e, owner)
                                && CombatTools.canBeHostileToMe(e, owner)
                ).stream()
                .sorted(Comparator.comparingDouble(this::distanceToSqr))
                .filter(e -> RaycastTools.hasLineOfSight(level, this, e))
                .findFirst()
                .orElse(null);
    }

    private void setStageByCurrentCharge() {
        if (currentChargeTick <= 0) {
            entityData.set(CHARGE_STAGE, 0);
        } else if (currentChargeTick <= 6) {
            entityData.set(CHARGE_STAGE, 1);
        } else if (currentChargeTick <= 9) {
            entityData.set(CHARGE_STAGE, 2);
        } else {
            entityData.set(CHARGE_STAGE, 3);
        }
    }

    public void setOwner(LivingEntity owner) {
        ownerUuid = owner.getUUID();
        cachedOwner = owner;
    }

    public LivingEntity getOwner() {
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }
        if (ownerUuid != null && level() instanceof ServerLevel serverLevel) {
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

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public void setRestBulletCount(int count) {
        restBulletCount = Math.max(0, count);
    }

    public int getRestBulletCount() {
        return restBulletCount;
    }

    public @Nullable String getOwnerName() {
        var owner = getOwner();
        return owner != null ? owner.getName().getString() : null;
    }

    public void setTurretMaxHealth(float maxHealth) {
        var attribute = getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(maxHealth);
        }
        setHealth(maxHealth);
    }

    public int getStage() {
        return entityData.get(CHARGE_STAGE);
    }

    public float getAimPitch() {
        return entityData.get(AIM_PITCH);
    }

    private Vec3 getAnchorCenter() {
        return new Vec3(anchorPos.getX() + 0.5, anchorPos.getY(), anchorPos.getZ() + 0.5);
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
                0,
                state -> {
                    state.setAnimation(IDLE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
