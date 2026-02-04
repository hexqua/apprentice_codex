package jp.aquafactory.apprenticecodex.common.spells.breachingenemy;

import jp.aquafactory.apprenticecodex.common.entity.spell.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.common.utility.CombatTools;
import jp.aquafactory.apprenticecodex.common.utility.EffectTools;
import jp.aquafactory.apprenticecodex.common.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.common.utility.RotationTools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class BreachingEnemyShotgunEntity extends SummonWeaponEntity {

    private float damage;
    private float range;

    public BreachingEnemyShotgunEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public BreachingEnemyShotgunEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
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
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putFloat("Damage", damage);
        pCompound.putFloat("Range", range);
    }

    @Override
    public void onClientRemoval() {
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

        if (level.isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        // ブリーチングエネミーは常に視線先を狙う.
        var aimResult = RaycastTools.raycastFromEye(owner, range, e -> CombatTools.isValidCombatTarget(e, this));
        var targetVec = aimResult.hitPosition().subtract(position());
        var yawPitch = RotationTools.calculateYawPitchByDirection(targetVec);
        setYRot(yawPitch.yaw());
        setXRot(yawPitch.pitch());
        hasImpulse = true;
    }

    @Override
    public Vec3 getStandbyPosition() {
        if ((getOwner() instanceof LivingEntity owner)) {
            return RotationTools.calculateBehindPosition(owner, -0.8, 1.1, 0.1);
        }

        return Vec3.ZERO;
    }

    public void setDamage(float newDamage) {
        damage = newDamage;
    }

    public void setRange(float newRange) {
        range = newRange;
    }
}
