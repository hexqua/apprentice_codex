package jp.aquafactory.apprenticecodex.recipe.crafting;

import jp.aquafactory.apprenticecodex.compat.patchouli.IronsGuideBookWorkaround;
import jp.aquafactory.apprenticecodex.registry.RecipeRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public final class IronsGuideBookRepairRecipe extends CustomRecipe {
    public IronsGuideBookRepairRecipe(ResourceLocation recipeId, CraftingBookCategory category) {
        super(recipeId, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return IronsGuideBookWorkaround.matchesOriginalIronsGuideBookRepairRecipe(container);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        var guideBookItem = ForgeRegistries.ITEMS.getValue(IronsGuideBookWorkaround.PATCHOULI_GUIDE_BOOK_ID);
        if (guideBookItem == null) {
            return ItemStack.EMPTY;
        }

        var result = new ItemStack(guideBookItem);
        IronsGuideBookWorkaround.bindToIronsGuideBook(result);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.IRONS_GUIDE_BOOK_REPAIR_SERIALIZER.get();
    }
}
