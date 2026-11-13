package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCompat;
import net.minecraft.client.Minecraft;
import net.stln.magitech.content.gui.overlay.ManaGaugeOverlay;
import net.stln.magitech.content.gui.overlay.RadialSpellMenuOverlay;
import net.stln.magitech.content.item.tool.toolitem.SpellCasterItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = {ManaGaugeOverlay.class, RadialSpellMenuOverlay.class}, remap = false)
public abstract class MagitechStaffOverlayMixin {
    @Definition(id = "SpellCasterItem", type = SpellCasterItem.class)
    @Expression("? instanceof SpellCasterItem")
    @ModifyExpressionValue(method = "render", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean apprenticecodex$recognizeStaff(boolean original) {
        var player = Minecraft.getInstance().player;
        // 満タン時のHUDと円形メニューの発動元選択も、独立した杖を認識させる。
        return original || player != null && MagiEngineersStaffCompat.isStaff(player.getMainHandItem());
    }
}
