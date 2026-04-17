package jp.aquafactory.apprenticecodex.compat.jei;

import jp.aquafactory.apprenticecodex.recipe.smithing.AlchemistsFlaskSmithingRecipe;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;
import net.minecraft.core.RegistryAccess;

// IIngredientAcceptor 自体は JEI 側で @ApiStatus.NonExtendable が付いていて、「mods がこれを自前実装するな」という意味.
// こちらの実装はそれを implements しておらず、ISmithingCategoryExtension を実装していて,
// その JEI API が要求するメソッド署名として <T extends IIngredientAcceptor<T>> を受け取っているだけ.
@SuppressWarnings("NonExtendableApiUsage")
public final class AlchemistsFlaskSmithingJeiExtension implements ISmithingCategoryExtension<AlchemistsFlaskSmithingRecipe> {
    @Override
    public <T extends IIngredientAcceptor<T>> void setTemplate(AlchemistsFlaskSmithingRecipe recipe, T builder) {
        builder.addIngredients(recipe.getTemplate());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setBase(AlchemistsFlaskSmithingRecipe recipe, T builder) {
        builder.addIngredients(recipe.getBase());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setAddition(AlchemistsFlaskSmithingRecipe recipe, T builder) {
        builder.addIngredients(recipe.getAddition());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setOutput(AlchemistsFlaskSmithingRecipe recipe, T builder) {
        builder.addItemStack(recipe.getResultItem(RegistryAccess.EMPTY));
    }
}
