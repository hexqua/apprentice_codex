package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.apprenticecodex.item.shield.ShieldCastUseContext;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ClientMagicData.class, remap = false)
public abstract class ClientMagicDataMixin {
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
