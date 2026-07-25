package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface InventoryInsertTarget {
    InsertHint getInventoryInsertHint(ItemStack storageStack, ItemStack incomingStack, Player player);

    static boolean canModifyStorageSlot(ItemStack storageStack, Slot slot, Player player) {
        return storageStack.getCount() == 1
                && slot.mayPickup(player)
                && slot.mayPlace(storageStack);
    }

    enum InsertHint {
        NONE,
        ITEM,
        MANA
    }
}
