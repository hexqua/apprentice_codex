package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public interface NonDamageableAnvilMergeItem {
    default boolean supportsSameItemAnvilMerge(ItemStack leftStack, ItemStack rightStack) {
        return leftStack.getItem() == rightStack.getItem();
    }

    default boolean isAnvilMergeEnchantmentAllowed(ItemStack stack, Enchantment enchantment) {
        return enchantment.canApplyAtEnchantingTable(stack);
    }
}
