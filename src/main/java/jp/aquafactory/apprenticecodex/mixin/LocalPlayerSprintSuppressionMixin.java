package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.event.client.PsychicDriveGauntletsUseInput;
import jp.aquafactory.apprenticecodex.event.client.SpellrifleSprintState;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSprintSuppressionMixin {
    @Inject(method = "canStartSprinting", at = @At("HEAD"), cancellable = true, require = 0)
    private void apprenticecodex$preventSuppressedSprint(CallbackInfoReturnable<Boolean> cir) {
        if (apprenticecodex$shouldSuppressSprint((LocalPlayer) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"), require = 0)
    private void apprenticecodex$stopSuppressedSprint(CallbackInfo ci) {
        var player = (LocalPlayer) (Object) this;
        // tick末尾だけの解除では、押しっぱなしのダッシュが毎tick再開して速度・FOVが揺れる。
        if (apprenticecodex$shouldSuppressSprint(player)) {
            player.setSprinting(false);
        }
    }

    @Unique
    private static boolean apprenticecodex$shouldSuppressSprint(LocalPlayer player) {
        return SpellrifleSprintState.isAiming(player) || PsychicDriveGauntletsUseInput.isCharging();
    }
}
