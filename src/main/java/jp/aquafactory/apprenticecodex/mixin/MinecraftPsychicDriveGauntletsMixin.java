package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.event.client.PsychicDriveGauntletsUseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftPsychicDriveGauntletsMixin {
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void apprenticecodex$cancelGauntletsOnScreen(@Nullable Screen screen, CallbackInfo ci) {
        // 画面を開くと同tick内でUseが離され得るため、通常のrelease処理より先に不発で解除する。
        if (screen != null) PsychicDriveGauntletsUseInput.cancel();
    }

    @Inject(method = "setWindowActive", at = @At("HEAD"))
    private void apprenticecodex$cancelGauntletsOnFocusLoss(boolean focused, CallbackInfo ci) {
        if (!focused) PsychicDriveGauntletsUseInput.cancel();
    }
}
