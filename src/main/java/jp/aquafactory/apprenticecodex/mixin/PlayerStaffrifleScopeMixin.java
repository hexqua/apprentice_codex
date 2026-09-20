package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.event.client.MultipurposeStaffrifleClientAdsState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerStaffrifleScopeMixin {
    @Inject(method = "isScoping", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$staffrifleScope(CallbackInfoReturnable<Boolean> cir) {
        // 射撃で使用状態が解除されても、ADS入力中はバニラの枠・感度・手元非表示を維持する。
        if (MultipurposeStaffrifleClientAdsState.isScoped((Player) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
