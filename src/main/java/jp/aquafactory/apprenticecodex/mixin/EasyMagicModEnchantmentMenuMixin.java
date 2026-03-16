package jp.aquafactory.apprenticecodex.mixin;

import jp.aquafactory.apprenticecodex.item.armor.EnchantressEnchantingTableBonusHelper;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "fuzs.easymagic.world.inventory.ModEnchantmentMenu", remap = false)
public abstract class EasyMagicModEnchantmentMenuMixin {
    @Shadow
    private Player player;

    @Inject(
            method = "updateLevelsAndClues(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At("RETURN"),
            require = 1
    )
    private void apprenticecodex$applyEnchantressBonus(RegistryAccess registryAccess, ItemStack itemStack, int enchantingPower, CallbackInfo ci) {
        if (EnchantressEnchantingTableBonusHelper.isFeatureDisabled()) {
            return;
        }

        var menu = (EnchantmentMenu) (Object) this;
        var targetRow = EnchantressEnchantingTableBonusHelper.TARGET_ENCHANT_ROW;
        if (menu.costs[targetRow] <= 0) {
            return;
        }

        var bonus = EnchantressEnchantingTableBonusHelper.getBonusForPlayer(player);
        if (bonus <= 0) {
            return;
        }

        menu.costs[targetRow] += bonus;
    }
}
