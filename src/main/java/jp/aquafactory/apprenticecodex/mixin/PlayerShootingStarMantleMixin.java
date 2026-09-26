package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerShootingStarMantleMixin {
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$startMantleFlight(CallbackInfoReturnable<Boolean> cir) {
        var player = (Player) (Object) this;
        if (ShootingStarMantleRuntime.isHovering(player)) {
            cir.setReturnValue(false);
        } else if (!player.onGround() && !player.isFallFlying() && ShootingStarMantleRuntime.canFly(player)) {
            player.startFallFlying();
            ShootingStarMantleRuntime.state(player).flying = true;
            cir.setReturnValue(true);
        }
    }
}
