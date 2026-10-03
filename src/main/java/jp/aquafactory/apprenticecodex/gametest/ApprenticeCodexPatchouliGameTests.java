package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.patchouli.PatchouliBookSupport;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ApprenticeCodexPatchouliGameTests {
    private ApprenticeCodexPatchouliGameTests() {
    }

    @GameTest(template = "gametest/basic_floor")
    public static void apprenticesMemoRecipeRespectsPatchouliAvailability(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        var book = PatchouliBookSupport.createBookStack();
        var recipe = recipes.byKey(PatchouliBookSupport.BOOK_ID);

        // 未導入時も同じテストを通し、任意依存のクラス読み込みとデータ読込の境界を確認する。
        if (!ModList.get().isLoaded("patchouli")) {
            helper.assertTrue(book.isEmpty(), "Patchouli book must be absent without Patchouli");
            helper.assertTrue(recipe.isEmpty(), "Patchouli book recipe must be skipped without Patchouli");
            helper.succeed();
            return;
        }

        helper.assertFalse(book.isEmpty(), "Patchouli book stack is missing");
        helper.assertTrue(recipe.isPresent(), "Apprentice's Memo recipe is missing");
        var input = CraftingInput.of(3, 3, List.of(
                ItemStack.EMPTY, new ItemStack(ItemRegistry.ARCANE_ESSENCE.get()), ItemStack.EMPTY,
                new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.BOOK), new ItemStack(Items.COPPER_INGOT),
                ItemStack.EMPTY, new ItemStack(Items.BLUE_DYE), ItemStack.EMPTY
        ));
        var matchingRecipe = recipes.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(matchingRecipe.isPresent(), "The shaped book recipe must craft Apprentice's Memo");
        helper.assertTrue(matchingRecipe.orElseThrow().id().equals(PatchouliBookSupport.BOOK_ID),
                "The shaped ingredients must match the Apprentice's Memo recipe");
        var result = matchingRecipe.orElseThrow().value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.isSameItemSameComponents(book, result),
                "Crafted Apprentice's Memo must retain its Patchouli book component");
        helper.assertTrue(result.getCount() == 1, "Apprentice's Memo recipe must produce one book");
        helper.succeed();
    }
}
