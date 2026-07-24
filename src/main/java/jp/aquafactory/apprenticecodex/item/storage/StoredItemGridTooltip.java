package jp.aquafactory.apprenticecodex.item.storage;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public interface StoredItemGridTooltip extends TooltipComponent {
    NonNullList<ItemStack> items();

    int highlightedIndex();

    boolean full();
}
