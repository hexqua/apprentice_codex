package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.shield.BulwarkGreatshield;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBulwarkGreatshieldMixin {
    @Inject(method = "isDamageSourceBlocked", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$allowBulwarkSpecialBlocks(
            DamageSource source,
            CallbackInfoReturnable<Boolean> cir
    ) {
        var defender = (LivingEntity) (Object) this;
        if (!defender.isBlocking() || !(defender.getUseItem().getItem() instanceof BulwarkGreatshield)
                || !apprentice_codex$isSupportedSpecialSource(source) || !apprentice_codex$isFromFront(defender, source)) {
            return;
        }
        // 1.20.1ではここを通さないと ShieldBlockEvent・盾統計・耐久処理が一切発生しない。
        cir.setReturnValue(true);
    }

    @Unique
    private static boolean apprentice_codex$isSupportedSpecialSource(DamageSource source) {
        var direct = source.getDirectEntity();
        var owner = source.getEntity();
        return direct instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0
                || source.is(DamageTypes.SONIC_BOOM) || direct instanceof Warden || owner instanceof Warden
                || !source.is(DamageTypes.THORNS) && (direct instanceof Guardian || owner instanceof Guardian);
    }

    @Unique
    private static boolean apprentice_codex$isFromFront(LivingEntity defender, DamageSource source) {
        Vec3 origin = source.getSourcePosition();
        if (origin == null) {
            return false;
        }
        var view = defender.getViewVector(1.0F);
        var incoming = origin.vectorTo(defender.position()).normalize();
        incoming = new Vec3(incoming.x, 0.0D, incoming.z);
        return incoming.dot(view) < 0.0D;
    }
}
