package jp.aquafactory.apprenticecodex.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.stln.magitech.content.gui.overlay.ManaGaugeOverlay;
import net.stln.magitech.content.item.tool.toolitem.SpellCasterItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ManaGaugeOverlay.class, remap = false)
public abstract class MagitechStaffOverlayMixin {
    @Definition(id = "SpellCasterItem", type = SpellCasterItem.class)
    @Expression("? instanceof SpellCasterItem")
    @ModifyExpressionValue(method = "render", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean apprenticecodex$recognizeStaff(boolean original) {
        var player = Minecraft.getInstance().player;
        // 表示可否と発動元の選択は分離し、別の上流杖へ専用性能を混ぜない。
        return original || player != null && (MagiEngineersStaffCompat.isStaff(player.getMainHandItem())
                || MagiEngineersStaffCompat.isStaff(player.getOffhandItem()));
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/stln/magitech/feature/magic/MagicPerformanceHelper;getEffectiveCost(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/stln/magitech/feature/magic/spell/ISpell;)F"),
            index = 1)
    private ItemStack apprenticecodex$costWand(ItemStack original) {
        var player = Minecraft.getInstance().player;
        return player == null ? original : MagiEngineersStaffCompat.displayWand(player);
    }
}
