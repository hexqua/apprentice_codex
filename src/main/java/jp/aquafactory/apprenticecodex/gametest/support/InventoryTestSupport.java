package jp.aquafactory.apprenticecodex.gametest.support;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public final class InventoryTestSupport {
    private InventoryTestSupport() {}

    public static boolean containsItem(ChestBlockEntity chest, Item item, int count) {
        for (var slot = 0; slot < chest.getContainerSize(); ++slot) {
            var stack = chest.getItem(slot);
            if (stack.is(item) && stack.getCount() == count) {
                return true;
            }
        }
        return false;
    }
}
