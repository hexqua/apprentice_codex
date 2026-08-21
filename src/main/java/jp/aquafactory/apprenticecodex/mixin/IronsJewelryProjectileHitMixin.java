package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import jp.aquafactory.apprenticecodex.compat.ironsjewelry.IronsJewelryProjectileHitCompat;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "io.redspace.ironsjewelry.event.JewelryBonusEvents", remap = false)
public abstract class IronsJewelryProjectileHitMixin {
    @Definition(id = "Projectile", type = Projectile.class)
    @Expression("? instanceof Projectile")
    @ModifyExpressionValue(
            method = "onLivingDamaged",
            at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private static boolean apprenticecodex$acceptTaggedHitscanDamage(
            boolean original,
            LivingIncomingDamageEvent event
    ) {
        // Jewelry本来のProjectile判定を維持し、実体弾を持たない銃撃だけdamage typeで補完する。
        return IronsJewelryProjectileHitCompat.isProjectileHit(original, event.getSource());
    }
}
