package jp.aquafactory.apprenticecodex.compat.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public record AlchemyBrewerJeiRecipe(
        ResourceLocation id,
        Ingredient base,
        Ingredient ingredient,
        @Nullable Ingredient modifier,
        ItemStack result,
        int fluidAmountMb,
        int processingTimeTicks
) {
}
