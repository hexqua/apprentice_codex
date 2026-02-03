package jp.aquafactory.apprenticecodex.common.spells.quickarms;

import jp.aquafactory.apprenticecodex.client.particles.MuzzleFlashParticleOptions;
import jp.aquafactory.apprenticecodex.common.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.common.registry.SpellsRegistry;
import jp.aquafactory.apprenticecodex.common.utility.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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

public class QuickArmsHandgunEntity  extends Entity implements TraceableEntity {

    private UUID ownerUUID;
    private Entity cachedOwner;

    private float damage;
    private float range;

    private boolean isStandbyFirstFire;
    private int standbyTick;

    public QuickArmsHandgunEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        setNoGravity(true);
    }

    public QuickArmsHandgunEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel);
        setOwner(owner);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        // do nothing.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        if (pCompound.hasUUID("OwnerUUID")) {
            ownerUUID = pCompound.getUUID("OwnerUUID");
            cachedOwner = null;
        }
        damage = pCompound.getFloat("Damage");
        range = pCompound.getFloat("Range");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        if (ownerUUID != null) {
            pCompound.putUUID("OwnerUUID", ownerUUID);
        }
        pCompound.putFloat("Damage", damage);
        pCompound.putFloat("Range", range);
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
                0.2f,
                8,
                0.01f,
                0.01,
                ParticleTypes.END_ROD,
                level
        );

        super.onClientRemoval();
    }

    @Override
    public void tick() {
        var level = level();

        // 射出時パーティクル(再ログインで消えるので制御不要)
        if (level.isClientSide && firstTick) {
            EffectTools.createRingParticleClient(
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

        if (level.isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        // クイックアームは常に視線先を狙う.
        var aimResult = RaycastTools.raycastFromEye(owner, range, e -> CombatTools.isValidCombatTarget(e, this));
        var targetVec = aimResult.hitPosition().subtract(position());
        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        hasImpulse = true;

        if (standbyTick > 0) {
            --standbyTick;
            if (standbyTick == 0 && isStandbyFirstFire) {
                isStandbyFirstFire = false;
                fire(level);
            }
        }
    }

    public void fire(Level level){
        if (!(getOwner() instanceof LivingEntity owner)){
            return;
        }

        var aimResult = RaycastTools.raycastFromEye(owner, range, e -> CombatTools.isValidCombatTarget(e, this));
        if (aimResult.hitEntity() != null) {
            var target = CombatTools.resolutePartEntity(aimResult.hitEntity());
            var source = CombatTools.getDamageSource(level(), this, getOwner(), "quick_arms_handgun");
            CombatTools.applyDamage(target, damage, source, SpellsRegistry.QUICK_ARMS.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
        }

        if (level instanceof ServerLevel server) {
            var target = aimResult.hitPosition();
            var firePosition = position().add(getLookAngle().scale(0.5));
            server.sendParticles(new MuzzleFlashParticleOptions(0.5f), firePosition.x, firePosition.y, firePosition.z, 0, 0, 0, 0, 0);

            switch (aimResult.hitType()) {
                case NONE:
                    // do nothing.
                    break;
                case BLOCK:
                    server.sendParticles(ParticleTypes.SMOKE, target.x, target.y, target.z, 2, .05, .05, .05, .05);
                    break;
                case LIVING_ENTITY:
                    server.sendParticles(ParticleTypes.ENCHANTED_HIT, target.x, target.y, target.z, 6, .15, .15, .15, .1);
                    break;
            }
        }

        AudioTools.playSoundFromEntity(level, this, SoundRegistry.HANDGUN.get(), SoundSource.PLAYERS, 1.0f);
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

    public void setDamage(float newDamage) {
        damage = newDamage;
    }

    public void setRange(float newRange) {
        range = newRange;
    }

    public void setFireStandby(int ticks) {
        isStandbyFirstFire = true;
        standbyTick = ticks;
    }

    public boolean canFire(){
        return standbyTick <= 0;
    }

    private static Vec3 getAimingPosition(LivingEntity owner) {
        var yawAngle = owner.getYRot() * Mth.DEG_TO_RAD;
        var forwardX = -Mth.sin(yawAngle);
        var forwardZ = Mth.cos(yawAngle);

        var back = new Vec3(-forwardX, 0, -forwardZ).normalize();
        var right = new Vec3(back.z, 0, -back.x).normalize();

        var behindOffset = back.scale(-0.6).add(new Vec3(0, -0.1, 0)).add(right.scale(0.9));
        return owner.getEyePosition().add(behindOffset);
    }
}
