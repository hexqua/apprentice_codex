package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.spell.remoteeye.RemoteEyeClientController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = net.minecraft.client.MouseHandler.class, priority = 1100)
public abstract class MouseHandlerMixin {

    // vanilla側の感度計算や tutorial.onMouse は維持したまま、
    // LocalPlayer.turn の直前だけを差し替えて EpicFight の Redirect と共存させる.
    @Inject(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/tutorial/Tutorial;onMouse(DD)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true,
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void apprenticecodex$turnRemoteEyeCamera(
            CallbackInfo ci,
            double time,
            double frameTime,
            double sensitivity,
            double cubicSensitivity,
            double turnScale,
            double yRot,
            double xRot,
            int invertY
    ) {
        if (RemoteEyeClientController.turnActiveCamera(yRot, xRot * invertY)) {
            ci.cancel();
        }
    }
}
