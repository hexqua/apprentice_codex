package jp.aquafactory.apprenticecodex.spell.tirovolley;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.particle.MuzzleFlashParticleOptions;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
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

public class TiroVolleyMusketEntity extends SummonWeaponEntity implements GeoEntity {
    public static final int MAX_RECOIL_TICK = 10;

    private static final RawAnimation APPEAR_TO_IDLE = RawAnimation.begin().thenPlay("appear").thenLoop("idle");
    // サーバー側はGeckoLibの実ロケーター行列を持たないため、geo上のmuzzle座標を姿勢から近似する。
    private static final Vec3 MUZZLE_LOCATOR = new Vec3(0.0, 3.0 / 16.0, -11.0 / 16.0);

    private static final EntityDataAccessor<Integer> RECOIL_TICK =
            SynchedEntityData.defineId(TiroVolleyMusketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FIRE_YAW =
            SynchedEntityData.defineId(TiroVolleyMusketEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> FIRE_PITCH =
            SynchedEntityData.defineId(TiroVolleyMusketEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> IS_FIRED =
            SynchedEntityData.defineId(TiroVolleyMusketEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage;
    private int spellLevel;
    private int fireDelayTick;
    private int recoilTick;
    private boolean fired;
    private @Nullable UUID targetId;
    private @Nullable Entity cachedTarget;

    public TiroVolleyMusketEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public TiroVolleyMusketEntity(EntityType<?> entityType, Level level, LivingEntity owner) {
        super(entityType, level, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RECOIL_TICK, 0);
        builder.define(FIRE_YAW, 0.0f);
        builder.define(FIRE_PITCH, 0.0f);
        builder.define(IS_FIRED, false);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        spellLevel = tag.getInt("SpellLevel");
        fireDelayTick = tag.getInt("FireDelayTick");
        recoilTick = tag.getInt("RecoilTick");
        fired = tag.getBoolean("Fired");
        targetId = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("SpellLevel", spellLevel);
        tag.putInt("FireDelayTick", fireDelayTick);
        tag.putInt("RecoilTick", recoilTick);
        tag.putBoolean("Fired", fired);
        if (targetId != null) {
            tag.putUUID("Target", targetId);
        }
    }

    public void setup(float damage, int spellLevel, int fireDelayTick, @Nullable Entity target) {
        this.damage = damage;
        this.spellLevel = spellLevel;
        this.fireDelayTick = fireDelayTick;
        if (target != null) {
            targetId = target.getUUID();
            cachedTarget = target;
            faceTarget(RaycastTools.getEntityTargetPosition(target));
        } else if (getOwner() instanceof LivingEntity owner) {
            setYRot(owner.getYRot());
            setXRot(owner.getXRot());
            setRot(getYRot(), getXRot());
            hasImpulse = true;
        }
    }

    @Override
    public void tick() {
        var level = level();
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticle(
                    position(),
                    getLookAngle(),
                    0.25f,
                    8,
                    0.01f,
                    0.01,
                    ParticleTypes.END_ROD,
                    level
            );
        }

        super.tick();
    }

    @Override
    public void onClientRemoval() {
        EffectTools.createStickParticle(
                position(),
                getLookAngle(),
                0.7,
                8,
                0.04f,
                0.01,
                ParticleTypes.END_ROD,
                level()
        );
        super.onClientRemoval();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (fired) {
            if (recoilTick > 0) {
                --recoilTick;
                entityData.set(RECOIL_TICK, recoilTick);
            } else {
                discard();
            }
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        var target = getLockedTarget(level);
        if (target != null) {
            faceTarget(RaycastTools.getEntityTargetPosition(target));
        } else {
            setYRot(owner.getYRot());
            setXRot(owner.getXRot());
            setRot(getYRot(), getXRot());
            hasImpulse = true;
        }

        --fireDelayTick;
        if (fireDelayTick <= 0) {
            tryFire(level, owner);
        }
    }

    private void tryFire(ServerLevel level, LivingEntity owner) {
        var target = getLockedTarget(level);
        if (target == null && targetId == null) {
            target = TiroVolley.findFallbackTarget(level, owner).orElse(null);
        }

        if (!isValidTarget(target, owner) || !hasClearShot(level, target)) {
            discard();
            return;
        }

        var hitPosition = RaycastTools.getEntityTargetPosition(target);
        faceTarget(hitPosition);
        setFireRotationByVector(hitPosition);

        var source = CombatTools.getDamageSource(level, this, owner, DamageTypes.TIRO_VOLLEY);
        CombatTools.applyDamage(
                target,
                resolveCurrentDamage(owner),
                source,
                SpellRegistry.TIRO_VOLLEY.get().getSchoolType(),
                CombatTools.KnockbackTypes.NO_KNOCKBACK
        );

        var muzzle = calculateMuzzlePosition();
        level.sendParticles(new MuzzleFlashParticleOptions(0.7f), muzzle.x, muzzle.y, muzzle.z, 0, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, hitPosition.x, hitPosition.y, hitPosition.z, 8, .18, .18, .18, .08);
        AudioTools.playSoundFromEntity(level, this, SoundRegistry.MUSKET.get(), SoundSource.PLAYERS, 1.0f);

        fired = true;
        recoilTick = MAX_RECOIL_TICK;
        entityData.set(IS_FIRED, true);
        entityData.set(RECOIL_TICK, recoilTick);
    }

    private boolean hasClearShot(ServerLevel level, Entity target) {
        var start = calculateMuzzlePosition();
        var end = RaycastTools.getEntityTargetPosition(target);
        var blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        return blockHit.getType() == HitResult.Type.MISS || blockHit.getLocation().distanceToSqr(start) >= end.distanceToSqr(start);
    }

    private boolean isValidTarget(@Nullable Entity target, LivingEntity owner) {
        return target != null
                && target.isAlive()
                && !target.isRemoved()
                && target.level() == level()
                && CombatTools.isValidCombatTarget(target, owner);
    }

    private @Nullable Entity getLockedTarget(ServerLevel level) {
        if (targetId == null) {
            return null;
        }
        if (cachedTarget != null && cachedTarget.isAlive() && !cachedTarget.isRemoved()) {
            return cachedTarget;
        }
        cachedTarget = level.getEntity(targetId);
        return cachedTarget;
    }

    private float resolveCurrentDamage(LivingEntity owner) {
        if (spellLevel > 0) {
            // FocusStaffbow などの詠唱中補正を拾うため、射撃直前のspell powerで再計算する。
            return TiroVolley.getDamage(SpellRegistry.TIRO_VOLLEY.get().getSpellPower(spellLevel, owner));
        }
        return damage;
    }

    private void faceTarget(Vec3 target) {
        var yawPitch = RotationTools.calculateYawPitchByDirection(target.subtract(position()));
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private void setFireRotationByVector(Vec3 target) {
        var yawPitch = RotationTools.calculateYawPitchByDirection(target.subtract(position()));
        entityData.set(FIRE_YAW, yawPitch.yaw());
        entityData.set(FIRE_PITCH, yawPitch.pitch());
    }

    private Vec3 calculateMuzzlePosition() {
        var forward = getLookAngle();
        if (forward.lengthSqr() < 1.0e-6) {
            forward = new Vec3(0, 0, 1);
        } else {
            forward = forward.normalize();
        }

        var worldUp = new Vec3(0, 1, 0);
        var right = worldUp.cross(forward);
        if (right.lengthSqr() < 1.0e-6) {
            right = new Vec3(1, 0, 0);
        }
        right = right.normalize();
        var up = forward.cross(right).normalize();

        return position()
                .add(right.scale(MUZZLE_LOCATOR.x))
                .add(up.scale(MUZZLE_LOCATOR.y))
                .add(forward.scale(-MUZZLE_LOCATOR.z));
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, 1.0, 0.0, 0.0);
        }
        return Vec3.ZERO;
    }

    public float getFireYaw() {
        return entityData.get(FIRE_YAW);
    }

    public float getFirePitch() {
        return entityData.get(FIRE_PITCH);
    }

    public int getRecoilTick() {
        return entityData.get(RECOIL_TICK);
    }

    public boolean isFired() {
        return entityData.get(IS_FIRED);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
                this, "main", 0,
                state -> {
                    state.setAnimation(APPEAR_TO_IDLE);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
