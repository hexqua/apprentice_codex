package jp.aquafactory.apprenticecodex.compat.jei;

import jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipe;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;

@SuppressWarnings("NonExtendableApiUsage")
public final class AlchemistsFlaskSmithingJeiExtension implements ISmithingCategoryExtension<AlchemistsFlaskSmithingRecipe> {
    @Override
    public <T extends IIngredientAcceptor<T>> void setTemplate(AlchemistsFlaskSmithingRecipe recipe, T ingredientAcceptor) {
        ingredientAcceptor.addIngredients(recipe.getTemplate());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setBase(AlchemistsFlaskSmithingRecipe recipe, T ingredientAcceptor) {
        ingredientAcceptor.addIngredients(recipe.getBase());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setAddition(AlchemistsFlaskSmithingRecipe recipe, T ingredientAcceptor) {
        ingredientAcceptor.addIngredients(recipe.getAddition());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setOutput(AlchemistsFlaskSmithingRecipe recipe, T ingredientAcceptor) {
        ingredientAcceptor.addItemStack(recipe.getResultTemplate());
    }
}
