package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowClientCastState;
import jp.aquafactory.apprenticecodex.item.shield.ShieldCastUseContext;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientMagicData.class, remap = false)
public abstract class ClientMagicDataMixin {
    @Inject(method = "handleCastDuration", at = @At("HEAD"), cancellable = true)
    private static void apprenticecodex$holdElementalBowDuration(CallbackInfo ci) {
        // 満了後も対象表示を残し、終了は server の発動・キャンセル通知に委ねる。
        if (ElementalBowClientCastState.isLocalActive()) ci.cancel();
    }

    @Redirect(
            method = "resetClientCastState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;stopUsingItem()V",
                    remap = true
            )
    )
    private static void apprenticecodex$preserveShieldUseDuringCastCleanup(LocalPlayer player) {
        // 完了 packet は魔法状態だけでなくローカル使用も解除するため、構えを維持する盾だけ除外する。
        if (!ShieldCastUseContext.shouldPreserveCurrentShieldUse(player)) {
            player.stopUsingItem();
        }
    }
}
