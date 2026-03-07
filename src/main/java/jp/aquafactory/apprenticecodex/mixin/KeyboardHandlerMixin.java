package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.spell.remoteeye.RemoteEyeClientController;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.glfw.GLFW;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$restrictRemoteEyeKeyPress(long windowPointer, int keyCode, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (!RemoteEyeClientController.shouldRestrictGameplayInput(minecraft)) {
            return;
        }

        if (action == GLFW.GLFW_RELEASE) {
            return;
        }

        if (RemoteEyeClientController.isAllowedRemoteEyeKey(minecraft, keyCode, scanCode)) {
            return;
        }

        // 押下/リピートだけ止め、リリースは通してキー状態の張り付きを避ける.
        ci.cancel();
    }
}
