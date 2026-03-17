package jp.aquafactory.apprenticecodex.compat.patchouli;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class IronsGuideBookWorkaround {
    public static final ResourceLocation PATCHOULI_GUIDE_BOOK_ID =
            ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book");
    public static final ResourceLocation VANILLA_BOOK_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "book");
    public static final ResourceLocation ARCANE_ESSENCE_ID =
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "arcane_essence");
    public static final String PATCHOULI_BOOK_TAG = "patchouli:book";
    public static final String IRONS_GUIDE_BOOK = "irons_spellbooks:iss_guide_book";

    private IronsGuideBookWorkaround() {
    }

    public static boolean isPatchouliGuideBook(ItemStack stack) {
        var itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return PATCHOULI_GUIDE_BOOK_ID.equals(itemKey);
    }

    public static boolean isUnboundPatchouliGuideBook(ItemStack stack) {
        if (!isPatchouliGuideBook(stack)) {
            return false;
        }

        var tag = stack.getTag();
        return tag == null || !tag.contains(PATCHOULI_BOOK_TAG, 8);
    }

    public static void bindToIronsGuideBook(ItemStack stack) {
        if (!isPatchouliGuideBook(stack)) {
            return;
        }

        stack.getOrCreateTag().putString(PATCHOULI_BOOK_TAG, IRONS_GUIDE_BOOK);
    }

    // Forge 1.20.1 の crafting_shapeless は result.components を読まないため、
    // 壊れたガイド本を材料構成から識別して手動で legacy NBT を補う。
    public static boolean matchesOriginalIronsGuideBookRecipe(Container container) {
        boolean foundBook = false;
        boolean foundArcaneEssence = false;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            var itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (VANILLA_BOOK_ID.equals(itemKey) && !foundBook) {
                foundBook = true;
                continue;
            }

            if (ARCANE_ESSENCE_ID.equals(itemKey) && !foundArcaneEssence) {
                foundArcaneEssence = true;
                continue;
            }

            return false;
        }

        return foundBook && foundArcaneEssence;
    }

    public static boolean matchesOriginalIronsGuideBookRepairRecipe(Container container) {
        boolean foundGuideBook = false;
        boolean foundArcaneEssence = false;

        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (isUnboundPatchouliGuideBook(stack) && !foundGuideBook) {
                foundGuideBook = true;
                continue;
            }

            var itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (ARCANE_ESSENCE_ID.equals(itemKey) && !foundArcaneEssence) {
                foundArcaneEssence = true;
                continue;
            }

            return false;
        }

        return foundGuideBook && foundArcaneEssence;
    }
}
