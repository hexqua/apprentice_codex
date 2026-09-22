package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerShootingStarMantleMixin {
    // LocalPlayerにはPlayer側とは別に胸装備の開始判定がある。
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;canElytraFly(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean apprenticecodex$allowMantle(boolean original) {
        return original || ShootingStarMantleRuntime.canFly((LocalPlayer) (Object) this);
    }
}
