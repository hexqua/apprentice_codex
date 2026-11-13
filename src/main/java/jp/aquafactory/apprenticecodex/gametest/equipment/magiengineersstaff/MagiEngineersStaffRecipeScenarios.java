package jp.aquafactory.apprenticecodex.gametest.equipment.magiengineersstaff;

import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.stln.magitech.content.item.ItemInit;
import net.stln.magitech.content.recipe.RecipeInit;
import net.stln.magitech.content.recipe.input.BaseAndIngredientsRecipeInput;

import java.util.ArrayList;
import java.util.List;

import static io.redspace.ironsspellbooks.registries.ItemRegistry.GRAYBEARD_STAFF;

final class MagiEngineersStaffRecipeScenarios {
    private MagiEngineersStaffRecipeScenarios() {
    }

    static void recipeSelection(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var base = new ItemStack(GRAYBEARD_STAFF.get());
        // 台座は順不同だが、各台座の要求個数を満たす必要がある。
        var ingredients = List.of(
                new ItemStack(ItemInit.FLUXIUM_INGOT.get(), 8),
                new ItemStack(ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get(), 8),
                new ItemStack(ItemInit.HIGH_PURITY_FLUORITE.get(), 8));
        var input = new BaseAndIngredientsRecipeInput(base, ingredients);
        var selected = manager.getRecipeFor(RecipeInit.INFUSION_TYPE.get(), input, helper.getLevel());
        helper.assertTrue(selected.isPresent(), "Complete ingredients must select an infusion recipe");
        var holder = selected.orElseThrow();
        helper.assertTrue(holder.id().equals(ItemRegistry.MAGI_ENGINEERS_STAFF.getId()),
                "Ingredients must select the Magi-Engineer's Staff recipe");
        var result = holder.value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ItemRegistry.MAGI_ENGINEERS_STAFF.get()) && result.getCount() == 1,
                "Infusion must produce exactly one Magi-Engineer's Staff");
        helper.assertTrue(holder.value().getMana() == 500_000L,
                "Infusion must require 500000 mana, beyond the initial altar capacity");
        for (int i = 0; i < ingredients.size(); i++) {
            var shortIngredients = new ArrayList<>(ingredients.stream().map(ItemStack::copy).toList());
            shortIngredients.get(i).shrink(1);
            helper.assertTrue(!holder.value().matches(new BaseAndIngredientsRecipeInput(base, shortIngredients), helper.getLevel()),
                    "An ingredient stack of seven must not satisfy the infusion recipe: " + i);
        }
        helper.assertTrue(!holder.value().matches(new BaseAndIngredientsRecipeInput(ItemStack.EMPTY, ingredients), helper.getLevel()),
                "Infusion must require the base staff");
        helper.succeed();
    }
}
