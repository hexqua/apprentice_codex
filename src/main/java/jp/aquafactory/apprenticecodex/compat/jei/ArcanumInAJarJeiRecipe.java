package jp.aquafactory.apprenticecodex.compat.jei;

import net.minecraft.world.item.ItemStack;

public record ArcanumInAJarJeiRecipe(
        ItemStack material,
        ItemStack product,
        int processingTimeTicks
) {
    public ArcanumInAJarJeiRecipe {
        material = material.copyWithCount(1);
        product = product.copyWithCount(1);
        processingTimeTicks = Math.max(1, processingTimeTicks);
    }
}
