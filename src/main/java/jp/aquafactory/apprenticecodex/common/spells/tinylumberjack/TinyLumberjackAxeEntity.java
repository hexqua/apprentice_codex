package jp.aquafactory.apprenticecodex.common.spells.tinylumberjack;

import jp.aquafactory.apprenticecodex.common.entity.spell.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.common.utility.RotationTools;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TinyLumberjackAxeEntity extends SummonWeaponEntity {

    public TinyLumberjackAxeEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public TinyLumberjackAxeEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    protected void defineSynchedData() {
        // do nothing.
    }

    @Override
    public void tick() {
        var level = level();
        super.tick();

        if (level.isClientSide) {
            return;
        }

        if (!(getOwner() instanceof LivingEntity owner)) {
            discard();
            return;
        }

        var locatePosition = calculateAxePosition(owner);
        followTargetPosition(locatePosition);
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
        setRot(getYRot(), getXRot());
        hasImpulse = true;
    }

    @Override
    public Vec3 getStandbyPosition() {
        if (getOwner() instanceof LivingEntity owner) {
            return calculateAxePosition(owner);
        }

        return Vec3.ZERO;
    }

    private static Vec3 calculateAxePosition(LivingEntity owner) {
        return RotationTools.calculateBehindPosition(owner, -0.4, 0.6, -0.4);
    }
}
