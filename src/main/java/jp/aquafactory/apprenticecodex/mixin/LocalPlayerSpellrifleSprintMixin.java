package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.event.client.SpellrifleSprintState;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSpellrifleSprintMixin {
    @Inject(method = "canStartSprinting", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$prioritizeAds(CallbackInfoReturnable<Boolean> cir) {
        if (SpellrifleSprintState.isAiming((LocalPlayer) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void apprenticecodex$stopSprintForAds(CallbackInfo ci) {
        var player = (LocalPlayer) (Object) this;
        // tick末尾だけの解除では、押しっぱなしのダッシュが毎tick再開して速度・FOVが揺れる。
        if (SpellrifleSprintState.isAiming(player)) {
            player.setSprinting(false);
        }
    }
}
