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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class QuickArmsHandgunEntity extends SummonWeaponEntity {

    private float damage;
    private float range;
    private int sneakPercent;
    private int lifeTicks;

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
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        damage = pCompound.getFloat("Damage");
        range = pCompound.getFloat("Range");
        sneakPercent = pCompound.getInt("SneakPercent");
        lifeTicks = pCompound.getInt("LifeTicks");
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
        pCompound.putFloat("Range", range);
        pCompound.putInt("SneakPercent", sneakPercent);
        pCompound.putInt("LifeTicks", lifeTicks);
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

        if (lifeTicks > 0) {
            --lifeTicks;
            if (lifeTicks <= 0) {
                discard();
            }
        }
    }

    public void fire(Level level){
        if (!(getOwner() instanceof LivingEntity owner)){
            return;
        }

        var aimResult = RaycastTools.raycastFromEye(owner, range, 0.5, e -> CombatTools.isValidCombatTarget(e, this));
        faceTarget(aimResult.hitPosition());
        if (aimResult.hitEntity() != null) {
            var target = CombatTools.resolutePartEntity(aimResult.hitEntity());
            var source = CombatTools.getDamageSource(level(), this, getOwner(), DamageTypes.QUICK_ARMS);
            var hasSneakBonus = shouldApplySneakBonus(target, owner);
            var finalDamage = damage * (hasSneakBonus ? sneakPercent / 100.0f : 1.0f);
            CombatTools.applyDamage(target, finalDamage, source, SpellRegistry.QUICK_ARMS.get().getSchoolType(), CombatTools.KnockbackTypes.DEFAULT);
            if (hasSneakBonus) {
                AudioTools.playSoundFromEntity(level, this, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 2.0f);
            }
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

    public void setDamage(float newDamage, int newSneakPercent) {
        damage = newDamage;
        sneakPercent = newSneakPercent;
    }

    public void setRange(float newRange) {
        range = newRange;
    }

    public void setLifetimeTicks(int ticks) {
        lifeTicks = ticks;
    }

    private void faceTarget(Vec3 target) {
        var targetVec = target.subtract(position());
        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    private boolean shouldApplySneakBonus(Entity target, LivingEntity owner) {
        if (target instanceof Mob mob && mob.getTarget() == owner) {
            return false;
        }

        if (target instanceof NeutralMob neutral) {
            var angerTarget = neutral.getPersistentAngerTarget();
            if (angerTarget != null && angerTarget.equals(owner.getUUID())) {
                return false;
            }

            return !neutral.isAngryAt(owner);
        }

        return true;
    }
}
