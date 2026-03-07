package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.spell.remoteeye.RemoteEyeClientController;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.client.MouseHandler.class)
public abstract class MouseHandlerMixin {

    // RemoteEye 中の視線入力だけをカメラへ渡し、本体回転は固定側へ任せる.
    @Redirect(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
            )
    )
    private void redirectTurnPlayer(LocalPlayer player, double yRot, double xRot) {
        if (RemoteEyeClientController.turnActiveCamera(yRot, xRot)) {
            return;
        }

        player.turn(yRot, xRot);
    }
}
