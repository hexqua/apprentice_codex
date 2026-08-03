package jp.aquafactory.apprenticecodex.spell.lethalassault;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.GunSpellTracerPacket;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class LethalAssaultRifleEntity extends SummonWeaponEntity implements AntiMagicSusceptible {
    public static final int MAX_RECOIL_TICK = 2;

    private static final int FIRING_NOT_STARTED = -1;
    private static final int FIRST_FIRE_TICK = 5;
    private static final int SECOND_FIRE_TICK = 8;
    private static final int LAST_FIRE_TICK = 11;
    private static final int DISCARD_TICK = 20;
    private static final float TRACER_SPEED_BLOCKS_PER_TICK = 24.0F;
    private static final float TRACER_LENGTH = 8.0F;

    private static final EntityDataAccessor<Integer> RECOIL_TICK =
            SynchedEntityData.defineId(LethalAssaultRifleEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> FIRE_YAW =
            SynchedEntityData.defineId(LethalAssaultRifleEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> FIRE_PITCH =
            SynchedEntityData.defineId(LethalAssaultRifleEntity.class, EntityDataSerializers.FLOAT);

    private float damage;
    private int recoilTick;
    private int firingTick = FIRING_NOT_STARTED;
    private boolean released;

    public LethalAssaultRifleEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public LethalAssaultRifleEntity(EntityType<?> entityType, Level level, LivingEntity owner) {
        super(entityType, level, owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RECOIL_TICK, 0);
        builder.define(FIRE_YAW, 0.0f);
        builder.define(FIRE_PITCH, 0.0f);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        recoilTick = tag.getInt("RecoilTick");
        firingTick = tag.contains("FiringTick") ? tag.getInt("FiringTick") : FIRING_NOT_STARTED;
        released = tag.getBoolean("Released");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("RecoilTick", recoilTick);
        tag.putInt("FiringTick", firingTick);
        tag.putBoolean("Released", released);
    }

    @Override
    public void onClientRemoval() {
        var level = level();
        EffectTools.createStickParticle(
                position(),
                getLookAngle(),
                1.5,
                12,
                0.1f,
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
                    0.2f,
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
    public void tickOnServer(ServerLevel level) {
        if (recoilTick > 0) {
            --recoilTick;
            entityData.set(RECOIL_TICK, recoilTick);
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        if (!released) {
            followTargetPosition(getStandbyPosition());
            faceTarget(resolveAimPosition(owner));
        }

        if (firingTick == FIRING_NOT_STARTED) {
            return;
        }

        ++firingTick;
        if (firingTick >= DISCARD_TICK) {
            discard();
            return;
        }

        if (shouldFire(firingTick)) {
            fire(level, owner);
            if (firingTick == LAST_FIRE_TICK) {
                released = true;
            }
        }
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return RotationTools.calculateBehindPosition(owner, -0.8, 1.1, 0.1);
        }

        return Vec3.ZERO;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void onAntiMagic(MagicData playerMagicData) {
        // リーサルアサルトはINSTANT且つリキャスト無しのため、カウンタースペル対応は銃側への直撃必須にする.
        discard();
    }

    public void startFiring(float newDamage) {
        damage = newDamage;
        firingTick = 0;
        released = false;
    }

    public boolean hasStartedFiringForGameTest() {
        return firingTick != FIRING_NOT_STARTED;
    }

    public int getRecoilTick() {
        return entityData.get(RECOIL_TICK);
    }

    public float getFireYaw() {
        return entityData.get(FIRE_YAW);
    }

    public float getFirePitch() {
        return entityData.get(FIRE_PITCH);
    }

    private void fire(Level level, LivingEntity owner) {
        var aimResult = RaycastTools.raycastFromEye(owner, LethalAssault.getRange(), 0.5, e -> CombatTools.isValidCombatTarget(e, this));
        faceTarget(aimResult.hitPosition());
        setFireRotationByVector(aimResult.hitPosition());

        if (aimResult.hitEntity() != null) {
            var target = CombatTools.resolutePartEntity(aimResult.hitEntity());
            var source = createCombatDamageSource(DamageTypes.LETHAL_ASSAULT);
            CombatTools.applyDamage(target, damage, source, SpellRegistry.LETHAL_ASSAULT.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
        }

        if (level instanceof ServerLevel server) {
            var target = aimResult.hitPosition();
            var normal = target.subtract(position()).normalize();
            var firePosition = position().add(normal.scale(1));
            server.sendParticles(new MuzzleFlashParticleOptions(1.0f), firePosition.x, firePosition.y, firePosition.z, 0, 0, 0, 0, 0);

            Networks.sendToTrackingEntityAndSelf(owner, new GunSpellTracerPacket(
                    firePosition,
                    target,
                    TRACER_SPEED_BLOCKS_PER_TICK,
                    TRACER_LENGTH
            ));

            switch (aimResult.hitType()) {
                case NONE:
                    break;
                case BLOCK:
                    server.sendParticles(ParticleTypes.SMOKE, target.x, target.y, target.z, 4, .1, .1, .1, .1);
                    break;
                case LIVING_ENTITY:
                    server.sendParticles(ParticleTypes.ENCHANTED_HIT, target.x, target.y, target.z, 16, .25, .25, .25, .1);
                    break;
            }
        }

        recoilTick = MAX_RECOIL_TICK;
        entityData.set(RECOIL_TICK, recoilTick);
        AudioTools.playSoundFromEntity(level, this, SoundRegistry.FULLAUTO_RIFLE.get(), SoundSource.PLAYERS, 1.0f);
    }

    private Vec3 resolveAimPosition(LivingEntity owner) {
        return RaycastTools.raycastFromEye(owner, LethalAssault.getRange(), 1, e -> CombatTools.isValidCombatTarget(e, this)).hitPosition();
    }

    private void faceTarget(Vec3 target) {
        var targetVec = target.subtract(position());
        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private void setFireRotationByVector(Vec3 target) {
        var targetVec = target.subtract(position());
        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        entityData.set(FIRE_YAW, yawPitch.yaw());
        entityData.set(FIRE_PITCH, yawPitch.pitch());
    }

    private static boolean shouldFire(int tick) {
        return tick == FIRST_FIRE_TICK || tick == SECOND_FIRE_TICK || tick == LAST_FIRE_TICK;
    }
}
