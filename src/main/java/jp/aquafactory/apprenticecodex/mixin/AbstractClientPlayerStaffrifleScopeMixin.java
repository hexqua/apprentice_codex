package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.event.client.MultipurposeStaffrifleClientAdsState;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerStaffrifleScopeMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$staffrifleScopeFov(CallbackInfoReturnable<Float> cir) {
        // バニラの望遠鏡FOVはisUsingItemも要求するため、射撃中だけズームが外れるのを防ぐ。
        if (MultipurposeStaffrifleClientAdsState.isScoped((AbstractClientPlayer) (Object) this)) {
            cir.setReturnValue(0.1F);
        }
    }
}
