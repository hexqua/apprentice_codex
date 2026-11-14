package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.compat.magitech.MagiEngineersStaffCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.stln.magitech.content.gui.overlay.RadialSpellMenuOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = RadialSpellMenuOverlay.class, remap = false)
public abstract class MagitechStaffRadialMenuMixin {
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target =
            "Lnet/stln/magitech/feature/magic/spell/ISpell;getTooltip(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;"),
            index = 2)
    private ItemStack apprenticecodex$tooltipWand(ItemStack original) {
        var player = Minecraft.getInstance().player;
        return player == null ? original : MagiEngineersStaffCompat.displayWand(player);
    }
}
