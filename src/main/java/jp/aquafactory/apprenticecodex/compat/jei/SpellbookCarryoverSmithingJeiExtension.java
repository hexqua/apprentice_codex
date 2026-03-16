package jp.aquafactory.apprenticecodex.compat.jei;

import jp.aquafactory.apprenticecodex.recipe.smithing.SpellbookCarryoverSmithingRecipe;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;
import net.minecraft.core.RegistryAccess;

public final class SpellbookCarryoverSmithingJeiExtension implements ISmithingCategoryExtension<SpellbookCarryoverSmithingRecipe> {
    @Override
    public <T extends IIngredientAcceptor<T>> void setTemplate(SpellbookCarryoverSmithingRecipe recipe, T builder) {
        builder.addIngredients(recipe.getTemplate());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setBase(SpellbookCarryoverSmithingRecipe recipe, T builder) {
        builder.addIngredients(recipe.getBase());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setAddition(SpellbookCarryoverSmithingRecipe recipe, T builder) {
        builder.addIngredients(recipe.getAddition());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setOutput(SpellbookCarryoverSmithingRecipe recipe, T builder) {
        builder.addItemStack(recipe.getResultItem(RegistryAccess.EMPTY));
    }
}
