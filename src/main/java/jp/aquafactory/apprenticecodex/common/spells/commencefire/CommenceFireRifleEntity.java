package jp.aquafactory.apprenticecodex.common.spells.commencefire;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import jp.aquafactory.apprenticecodex.common.registry.DamageSources;
import jp.aquafactory.apprenticecodex.common.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.common.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.common.utility.AudioTools;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import jp.aquafactory.apprenticecodex.common.utility.EffectTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CommenceFireRifleEntity extends Entity implements TraceableEntity {

    public enum HitTypes{
        MISS,
        BLOCK,
        ENTITY,
    }

    public static final int MAX_RECOIL_TICK = 8;

    private static final EntityDataAccessor<Integer> RECOIL_TICK =
            SynchedEntityData.defineId(CommenceFireRifleEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> FIRE_YAW =
            SynchedEntityData.defineId(CommenceFireRifleEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> FIRE_PITCH =
            SynchedEntityData.defineId(CommenceFireRifleEntity.class, EntityDataSerializers.FLOAT);

    private UUID ownerUUID;

    private Entity cachedOwner;
    private Vec3 aimPosition;
    private float damage;
    private int castingTick;
    private int recoilTick;

    public CommenceFireRifleEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setNoGravity(true);
    }

    public CommenceFireRifleEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel);
        setOwner(owner);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(RECOIL_TICK, 0);
        entityData.define(FIRE_YAW, 0.0f);
        entityData.define(FIRE_PITCH, 0.0f);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            ownerUUID = tag.getUUID("Owner");
            cachedOwner = null;
        }
        damage = tag.getFloat("Damage");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
        tag.putFloat("Damage", damage);
    }

    @Override
    public @Nullable Entity getOwner() {
        @SuppressWarnings("resource") var level = level();
        if (cachedOwner != null && !cachedOwner.isRemoved()) {
            return cachedOwner;
        }

        if (ownerUUID != null && level instanceof ServerLevel server) {
            cachedOwner = server.getEntity(ownerUUID);
            return cachedOwner;
        }

        return null;
    }

    public void setOwner(Entity pOwner) {
        if (pOwner != null) {
            ownerUUID = pOwner.getUUID();
            cachedOwner = pOwner;
        }
    }

    @Override
    public void onClientRemoval(){
        var level = level();
        EffectTools.createStickParticleClient(
                position(),
                getLookAngle(),
                2,
                16,
                0.1f,
                0.02,
                ParticleTypes.END_ROD, level
        );

        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();

        // 射出時パーティクル.
        // todo:再ログイン制御がいるかどうか.
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticleClient(
                    position(),
                    getLookAngle(),
                    0.3f,
                    12,
                    0.015f,
                    0.01,
                    ParticleTypes.END_ROD,
                    level
            );
        }

        super.tick();

        if (level.isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        if (recoilTick > 0) {
            --recoilTick;
            entityData.set(RECOIL_TICK, recoilTick);
        }

        var locatePosition = getAimingPosition(owner);
        var targetVec = locatePosition.subtract(position());
        var distance = targetVec.length();
        var step = targetVec.normalize().scale(Math.min(0.5, distance));

        if (distance < 0.001 || distance > 0.5) {
            setDeltaMovement(Vec3.ZERO);
            setPos(locatePosition.x, locatePosition.y, locatePosition.z);
        } else {
            setDeltaMovement(step);
            move(net.minecraft.world.entity.MoverType.SELF, step);
        }

        if (aimPosition != null) {
            var targetFaceVector = aimPosition.subtract(position()).normalize();
            var yaw = (float) (Mth.atan2(-targetFaceVector.x, targetFaceVector.z) * Mth.RAD_TO_DEG);
            var xzLen = Math.sqrt(targetFaceVector.x * targetFaceVector.x + targetFaceVector.z * targetFaceVector.z);
            var pitch = (float) (Mth.atan2(-targetFaceVector.y, xzLen) * Mth.RAD_TO_DEG);

            setYRot(yaw);
            setXRot(pitch);
        } else {
            setYRot(owner.getYRot());
            setXRot(0);
        }

        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    public void setDamage(float newDamage){
        damage = newDamage;
    }

    public void playCastingReticleEffect(int tick, int maxTick, Vec3 aimPosition,Level level) {
        // todo:発動者以外に見せたくないため、いずれクライアント専用にしつつオーナーにのみ見せるようにする.
        var radius = 1.0 - tick / (double) maxTick;
        var count = 20 - Math.round(15 * tick / (float) maxTick);
        var targetVec = aimPosition.subtract(position());
        EffectTools.createRingParticleServer(aimPosition, targetVec, radius, count, 0, 0, ParticleRegistry.RETICLE_DOT.get(), level);
    }


    public void setLookTarget(Vec3 target){
        aimPosition = target;
    }

    public void damageTarget(Entity target, Level level){
        var resoluteTarget = CombatTools.resolutePartEntity(target);
        var source = DamageSources.getDamageSource(level, getOwner(), "commence_fire");
        CombatTools.applyDamage(resoluteTarget, damage, source, SchoolRegistry.LIGHTNING.get(), CombatTools.KnockbackTypes.DEFAULT);
    }

    public void fire(Vec3 target, Level level, HitTypes hitType) {
        recoilTick = MAX_RECOIL_TICK;
        entityData.set(RECOIL_TICK, recoilTick);
        setFireRotationByVector(aimPosition);

        if (level instanceof ServerLevel server) {
            var targetVec = target.subtract(position());
            var normal = targetVec.normalize();
            var firePosition = position().add(normal.scale(1));
            server.sendParticles(ParticleRegistry.MUZZLE_FLASH.get(), firePosition.x, firePosition.y, firePosition.z, 0, 0, 0, 0, 0);

            switch (hitType){
                case MISS:
                    // do nothing.
                    break;
                case BLOCK:
                    server.sendParticles(ParticleTypes.SMOKE, target.x, target.y, target.z, 4, .1, .1, .1, .1);
                    break;
                case ENTITY:
                    server.sendParticles(ParticleTypes.CRIT, target.x, target.y, target.z, 16, .25, .25, .25, .1);
                    break;
            }
        }

        AudioTools.playSoundFromEntity(level, this, SoundRegistry.RIFLE.get(), SoundSource.PLAYERS, 1.0f);
        aimPosition = null;
    }

    public void locateAimingPosition(){
        if ((getOwner() instanceof LivingEntity owner)) {
            var formationPosition = getAimingPosition(owner);
            setPos(formationPosition.x, formationPosition.y, formationPosition.z);
            setYRot(owner.getYRot());
            setXRot(0);
            setRot(getYRot(), getXRot());
            hasImpulse = true;
        }
    }

    private void setFireRotationByVector(Vec3 aimPosition){
        var targetVec = aimPosition.subtract(position());
        var yaw = (float) (Mth.atan2(-targetVec.x, targetVec.z) * Mth.RAD_TO_DEG);
        var xzLen = Math.sqrt(targetVec.x * targetVec.x + targetVec.z * targetVec.z);
        var pitch = (float) (Mth.atan2(-targetVec.y, xzLen) * Mth.RAD_TO_DEG);
        entityData.set(FIRE_YAW, yaw);
        entityData.set(FIRE_PITCH, pitch);
    }

    public float getFireYaw(){
        return entityData.get(FIRE_YAW);
    }

    public float getFirePitch(){
        return entityData.get(FIRE_PITCH);
    }

    public int getRecoilTick(){
        return entityData.get(RECOIL_TICK);
    }

    public boolean duringRecoil(){
        return recoilTick > 0;
    }

    private static Vec3 getAimingPosition(LivingEntity owner) {
        var yawAngle = owner.getYRot() * Mth.DEG_TO_RAD;
        var forwardX = -Mth.sin(yawAngle);
        var forwardZ = Mth.cos(yawAngle);

        var back = new Vec3(-forwardX, 0, -forwardZ).normalize();
        var right = new Vec3(back.z, 0, -back.x).normalize();

        var behindOffset = back.scale(-0.3).add(new Vec3(0, 0.2, 0)).add(right.scale(-0.9));
        return owner.getEyePosition().add(behindOffset);
    }
}
