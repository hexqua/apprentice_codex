package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.event.client.ElementalBowInventoryOverlayRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsElementalBowOverlayMixin {
    @Inject(
            method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;isBarVisible()Z"
            )
    )
    private void apprentice_codex$renderElementalBowOverlay(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci) {
        // Forge の IItemDecorator は耐久バー描画後に呼ばれるため、表示順要件はここで満たす。
        ElementalBowInventoryOverlayRenderer.renderIfPresent((GuiGraphics) (Object) this, stack, x, y);
    }
}
