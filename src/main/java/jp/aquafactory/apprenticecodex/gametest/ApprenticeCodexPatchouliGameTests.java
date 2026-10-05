package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.patchouli.PatchouliBookSupport;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

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
        var input = new TransientCraftingContainer(new AbstractContainerMenu(null, 0) {
            @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        }, 3, 3);
        var items = List.of(
                ItemStack.EMPTY, new ItemStack(ItemRegistry.ARCANE_ESSENCE.get()), ItemStack.EMPTY,
                new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.BOOK), new ItemStack(Items.COPPER_INGOT),
                ItemStack.EMPTY, new ItemStack(Items.BLUE_DYE), ItemStack.EMPTY
        );
        for (int slot = 0; slot < items.size(); slot++) input.setItem(slot, items.get(slot));
        var matchingRecipe = recipes.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(matchingRecipe.isPresent(), "The shaped book recipe must craft Apprentice's Memo");
        helper.assertTrue(matchingRecipe.orElseThrow().getId().equals(PatchouliBookSupport.BOOK_ID),
                "The shaped ingredients must match the Apprentice's Memo recipe");
        var result = matchingRecipe.orElseThrow().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.isSameItemSameTags(book, result),
                "Crafted Apprentice's Memo must retain its Patchouli book NBT");
        helper.assertTrue(result.getCount() == 1, "Apprentice's Memo recipe must produce one book");
        helper.succeed();
    }
}
