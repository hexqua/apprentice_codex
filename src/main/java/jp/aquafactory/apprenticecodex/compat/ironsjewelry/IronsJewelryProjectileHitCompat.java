package jp.aquafactory.apprenticecodex.compat.ironsjewelry;

import jp.aquafactory.apprenticecodex.damage.ApprenticeDamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

public final class IronsJewelryProjectileHitCompat {
    private IronsJewelryProjectileHitCompat() {
    }

    public static boolean isProjectileHit(boolean original, DamageSource source) {
        return original || source.is(ApprenticeDamageTypeTags.TRIGGERS_IRONS_JEWELRY_PROJECTILE_HIT);
    }
}
