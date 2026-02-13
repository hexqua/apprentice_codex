package jp.aquafactory.apprenticecodex.spell.flyswatter;

import jp.aquafactory.apprenticecodex.entity.SummonWeaponEntity;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FlySwatterLauncherEntity extends SummonWeaponEntity {
    public FlySwatterLauncherEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public FlySwatterLauncherEntity(EntityType<?> pEntityType, Level pLevel, LivingEntity owner) {
        super(pEntityType, pLevel, owner);
    }

    @Override
    public Vec3 getStandbyPosition() {
        if ((getOwner() instanceof LivingEntity owner)) {
            return getAimingPosition(owner);
        }

        return Vec3.ZERO;
    }

    @Override
    protected void defineSynchedData() {
        // todo:表示同期周りで色々追加する.
    }

    private static Vec3 getAimingPosition(LivingEntity owner) {
        return RotationTools.calculateBehindPosition(owner, -0.3, -0.9, 0.2);
    }
}
