package jp.aquafactory.apprenticecodex.spell.silentassassin;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.particle.MuzzleFlashParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.AudioTools;
import jp.aquafactory.apprenticecodex.utility.CombatTools;
import jp.aquafactory.apprenticecodex.utility.EffectTools;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SilentAssassinRifleEntity extends SummonWeaponEntity {
    public enum HitTypes {
        MISS,
        BLOCK,
        ENTITY,
    }

    public static final int MAX_RECOIL_TICK = 10;

    private static final EntityDataAccessor<Integer> CASTING_TICK =
            SynchedEntityData.defineId(SilentAssassinRifleEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> MAX_CASTING_TICK =
            SynchedEntityData.defineId(SilentAssassinRifleEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> AIM_X =
            SynchedEntityData.defineId(SilentAssassinRifleEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> AIM_Y =
            SynchedEntityData.defineId(SilentAssassinRifleEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> AIM_Z =
            SynchedEntityData.defineId(SilentAssassinRifleEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> RECOIL_TICK =
            SynchedEntityData.defineId(SilentAssassinRifleEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> FIRE_YAW =
            SynchedEntityData.defineId(SilentAssassinRifleEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> FIRE_PITCH =
            SynchedEntityData.defineId(SilentAssassinRifleEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> IS_RELEASED =
            SynchedEntityData.defineId(SilentAssassinRifleEntity.class, EntityDataSerializers.BOOLEAN);

    private Vec3 aimPosition;
    private int recoilTick;
    private boolean isReleased;

    public SilentAssassinRifleEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public SilentAssassinRifleEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(CASTING_TICK, 0);
        entityData.define(MAX_CASTING_TICK, 0);
        entityData.define(AIM_X, 0.0f);
        entityData.define(AIM_Y, 0.0f);
        entityData.define(AIM_Z, 0.0f);
        entityData.define(RECOIL_TICK, 0);
        entityData.define(FIRE_YAW, 0.0f);
        entityData.define(FIRE_PITCH, 0.0f);
        entityData.define(IS_RELEASED, false);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        recoilTick = tag.getInt("RecoilTick");
        isReleased = tag.getBoolean("IsReleased");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RecoilTick", recoilTick);
        tag.putBoolean("IsReleased", isReleased);
    }

    @Override
    public void onClientRemoval() {
        var level = level();
        EffectTools.createStickParticle(
                position(),
                getLookAngle(),
                2.5,
                12,
                0.08f,
                0.02,
                ParticleTypes.END_ROD,
                level
        );

        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();

        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticle(
                    position(),
                    getLookAngle(),
                    0.25f,
                    10,
                    0.01f,
                    0.01,
                    ParticleTypes.END_ROD,
                    level
            );
        }

        if (level.isClientSide) {
            int castingTick = entityData.get(CASTING_TICK);
            if (castingTick > 0) {
                var maxCastingTick = entityData.get(MAX_CASTING_TICK);
                var targetPosition = new Vec3(entityData.get(AIM_X), entityData.get(AIM_Y), entityData.get(AIM_Z));
                var targetVec = targetPosition.subtract(position());
                var radius = 1.0 - castingTick / (double) maxCastingTick;
                var count = 24 - Math.round(18 * castingTick / (float) maxCastingTick);
                EffectTools.createRingParticle(targetPosition, targetVec, radius, count, 0, 0, ParticleRegistry.RETICLE_DOT.get(), level);
            }
        }

        super.tick();
    }

    @Override
    public void tickOnServer(ServerLevel level) {
        if (isReleased) {
            if (recoilTick > 0) {
                --recoilTick;
                entityData.set(RECOIL_TICK, recoilTick);
            } else {
                releaseWeapon();
            }
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        followTargetPosition(getStandbyPosition());

        if (aimPosition != null) {
            faceTarget(aimPosition);
        } else {
            setYRot(owner.getYRot());
            setXRot(0);
            setRot(getYRot(), getXRot());
            hasImpulse = true;
        }
    }

    public void setCastingReticleEffect(int tick, int maxTick, Vec3 target) {
        aimPosition = target;
        entityData.set(CASTING_TICK, tick);
        entityData.set(MAX_CASTING_TICK, maxTick);
        entityData.set(AIM_X, (float) target.x);
        entityData.set(AIM_Y, (float) target.y);
        entityData.set(AIM_Z, (float) target.z);
    }

    public void damageTarget(Entity target, float finalDamage, Level level) {
        var source = CombatTools.getDamageSource(level, this, getOwner(), DamageTypes.SILENT_ASSASSIN);
        CombatTools.applyDamage(target, finalDamage, source, SpellRegistry.SILENT_ASSASSIN.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
    }

    public void fire(Vec3 target, Level level, HitTypes hitType, boolean isHeadShot, boolean hasUnawareBonus) {
        recoilTick = MAX_RECOIL_TICK;
        isReleased = true;
        entityData.set(RECOIL_TICK, recoilTick);
        entityData.set(IS_RELEASED, true);
        setFireRotationByVector(target);

        if (level instanceof ServerLevel server) {
            var targetVec = target.subtract(position());
            var normal = targetVec.normalize();
            var firePosition = position().add(normal.scale(1.5));
            server.sendParticles(new MuzzleFlashParticleOptions(0.7f), firePosition.x, firePosition.y, firePosition.z, 0, 0, 0, 0, 0);

            switch (hitType) {
                case MISS:
                    break;
                case BLOCK:
                    server.sendParticles(ParticleTypes.SMOKE, target.x, target.y, target.z, 2, .05, .05, .05, .05);
                    break;
                case ENTITY:
                    server.sendParticles(ParticleTypes.ENCHANTED_HIT, target.x, target.y, target.z, 10, .2, .2, .2, .08);
                    break;
            }

            if (isHeadShot) {
                AudioTools.playSoundFromEntity(level, this, SoundRegistry.VANILLA_CRITICAL_SHOT.get(), SoundSource.PLAYERS, 1.0f, 2.0f);
                server.sendParticles(ParticleTypes.CRIT, target.x, target.y, target.z, 20, .3, .3, .3, .15);
            } else if (hasUnawareBonus) {
                server.sendParticles(ParticleTypes.CRIT, target.x, target.y, target.z, 8, .2, .2, .2, .08);
            }
        }

        AudioTools.playSoundFromEntity(level, this, SoundRegistry.SUPPRESS_RIFLE.get(), SoundSource.PLAYERS, 1.0f);
        entityData.set(CASTING_TICK, 0);
        aimPosition = null;
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, -0.8, 1.1, 0.1);
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

    public boolean getIsReleased() {
        return entityData.get(IS_RELEASED);
    }

    private void faceTarget(Vec3 target) {
        var targetVec = target.subtract(position());
        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private void setFireRotationByVector(Vec3 aimPosition) {
        var targetVec = aimPosition.subtract(position());
        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        entityData.set(FIRE_YAW, yawPitch.yaw());
        entityData.set(FIRE_PITCH, yawPitch.pitch());
    }
}
