package jp.aquafactory.apprenticecodex.spell.quickarms;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.particle.MuzzleFlashParticleOptions;
import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.registry.SoundRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class QuickArmsHandgunEntity extends SummonWeaponEntity {

    private float range;

    private float standbyDamage;
    private boolean isStandbyFirstFire;
    private int standbyTick;

    public QuickArmsHandgunEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public QuickArmsHandgunEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        // do nothing.
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        range = pCompound.getFloat("Range");
        standbyDamage = pCompound.getFloat("StandbyDamage");
        isStandbyFirstFire = pCompound.getBoolean("IsStandbyFirstFire");
        standbyTick = pCompound.getInt("StandbyTick");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Range", range);
        pCompound.putFloat("StandbyDamage", standbyDamage);
        pCompound.putBoolean("IsStandbyFirstFire", isStandbyFirstFire);
        pCompound.putInt("StandbyTick", standbyTick);
    }

    @Override
    public void onClientRemoval(){
        var level = level();
        EffectTools.createStickParticle(
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
        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        // クイックアームは常に視線先を狙う.
        var aimResult = RaycastTools.raycastFromEye(owner, range, 1, e -> CombatTools.isValidCombatTarget(e, this));
        faceTarget(aimResult.hitPosition());

        if (standbyTick > 0) {
            --standbyTick;
            if (standbyTick == 0 && isStandbyFirstFire) {
                isStandbyFirstFire = false;
                fire(level, standbyDamage);
            }
        }
    }

    public void fire(Level level, float damage){
        if (!(getOwner() instanceof LivingEntity owner)){
            return;
        }

        var aimResult = RaycastTools.raycastFromEye(owner, range, 0.5, e -> CombatTools.isValidCombatTarget(e, this));
        faceTarget(aimResult.hitPosition());
        if (aimResult.hitEntity() != null) {
            var target = CombatTools.resolutePartEntity(aimResult.hitEntity());
            var source = createCombatDamageSource(DamageTypes.QUICK_ARMS);
            CombatTools.applyDamage(target, damage, source, SpellRegistry.QUICK_ARMS.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
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

    @Override
    public Vec3 getStandbyPosition() {
        if ((getOwner() instanceof LivingEntity owner)) {
            return RotationTools.calculateBehindPosition(owner, -0.6, 0.9, -0.1);
        }

        return Vec3.ZERO;
    }

    public void setRange(float newRange) {
        range = newRange;
    }

    public void setFireStandby(int ticks, float firstFireDamage) {
        isStandbyFirstFire = true;
        standbyTick = ticks;
        standbyDamage = firstFireDamage;
    }

    public boolean canFire(){
        return standbyTick <= 0;
    }

    private void faceTarget(Vec3 target) {
        var targetVec = target.subtract(position());
        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }
}
