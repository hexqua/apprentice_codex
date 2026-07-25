package jp.aquafactory.apprenticecodex.item.curios.spellcasterammopouch;

import jp.aquafactory.apprenticecodex.item.storage.StoredItemGridTooltip;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public record SpellcasterAmmoPouchTooltip(
        NonNullList<ItemStack> items,
        int highlightedIndex,
        boolean full
) implements StoredItemGridTooltip {
}
