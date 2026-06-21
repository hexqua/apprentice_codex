package jp.aquafactory.apprenticecodex.spell.linearbuild;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

public final class LinearBuildItemSources {
    private LinearBuildItemSources() {
    }

    public static LinearBuildItemSource itemHandler(IItemHandler handler, Component label, boolean notifyRetrieved) {
        return new ItemHandlerSource(handler, label, notifyRetrieved);
    }

    public static LinearBuildItemSource inventoryStackItemHandler(
            IItemHandler handler,
            Component label,
            Inventory inventory
    ) {
        return new InventoryStackItemHandlerSource(handler, label, inventory);
    }

    public static boolean isSameItemIgnoringEmptyTag(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty() || !left.is(right.getItem())) {
            return false;
        }
        if (left.isDamageableItem() && left.getDamageValue() != right.getDamageValue()) {
            return false;
        }
        var leftTag = normalizedTag(left);
        var rightTag = normalizedTag(right);
        if (leftTag == null) {
            return rightTag == null;
        }
        return leftTag.equals(rightTag);
    }

    private static CompoundTag normalizedTag(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null || tag.isEmpty() ? null : tag;
    }

    private static class ItemHandlerSource implements LinearBuildItemSource {
        private final IItemHandler handler;
        private final Component label;
        private final boolean notifyRetrieved;

        private ItemHandlerSource(IItemHandler handler, Component label, boolean notifyRetrieved) {
            this.handler = handler;
            this.label = label;
            this.notifyRetrieved = notifyRetrieved;
        }

        @Override
        public Component label() {
            return label;
        }

        @Override
        public boolean shouldNotifyRetrieved() {
            return notifyRetrieved;
        }

        @Override
        public boolean hasMatchingItem(ItemStack template) {
            return findSlot(template) >= 0;
        }

        @Override
        public boolean consumeOne(ItemStack template) {
            var slot = findSlot(template);
            return slot >= 0 && !handler.extractItem(slot, 1, false).isEmpty();
        }

        private int findSlot(ItemStack template) {
            for (var slot = 0; slot < handler.getSlots(); ++slot) {
                var stack = handler.getStackInSlot(slot);
                if (isSameItemIgnoringEmptyTag(stack, template) && !handler.extractItem(slot, 1, true).isEmpty()) {
                    return slot;
                }
            }
            return -1;
        }
    }

    private static final class InventoryStackItemHandlerSource extends ItemHandlerSource {
        private final Inventory inventory;

        private InventoryStackItemHandlerSource(IItemHandler handler, Component label, Inventory inventory) {
            super(handler, label, true);
            this.inventory = inventory;
        }

        @Override
        public boolean consumeOne(ItemStack template) {
            var consumed = super.consumeOne(template);
            if (consumed) {
                inventory.setChanged();
            }
            return consumed;
        }
    }
}
