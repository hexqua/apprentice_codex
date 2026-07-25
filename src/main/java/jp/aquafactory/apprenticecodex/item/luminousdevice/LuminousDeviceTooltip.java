package jp.aquafactory.apprenticecodex.item.luminousdevice;

import jp.aquafactory.apprenticecodex.item.storage.StoredItemGridTooltip;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public record LuminousDeviceTooltip(
        NonNullList<ItemStack> items,
        int highlightedIndex,
        boolean full
) implements StoredItemGridTooltip {
}
