package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.utility.SchoolAffinityTooltipHelper;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.neoforged.neoforge.fluids.FluidStack;

@Mixin(FluidStack.class)
public abstract class FluidStackMixin {
    @Inject(
            method = "getHoverName()Lnet/minecraft/network/chat/Component;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void apprenticecodex$overrideSchoolAffinityFluidHoverName(CallbackInfoReturnable<Component> cir) {
        var description = SchoolAffinityTooltipHelper.tryBuildFluidDescription((FluidStack) (Object) this);
        if (description == null) {
            return;
        }
        cir.setReturnValue(description);
    }
}
