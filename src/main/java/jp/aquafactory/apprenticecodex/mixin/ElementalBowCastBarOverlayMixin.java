package jp.aquafactory.apprenticecodex.mixin;

import io.redspace.ironsspellbooks.gui.overlays.CastBarOverlay;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowClientCastState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CastBarOverlay.class, remap = false)
public abstract class ElementalBowCastBarOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void apprenticecodex$useBowCastBar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ElementalBowClientCastState.isLocalActive()) ci.cancel();
    }
}
