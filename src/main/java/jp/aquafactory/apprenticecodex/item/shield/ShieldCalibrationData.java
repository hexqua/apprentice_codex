package jp.aquafactory.apprenticecodex.item.shield;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

final class ShieldCalibrationData {
    private static final String ADJUSTMENTS_TAG = "Adjustments";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";

    private ShieldCalibrationData() {
    }

    static @NotNull ItemStack get(ItemStack stack, String calibrationKey, int slot, int slotCount) {
        if (stack.isEmpty() || slot < 0 || slot >= slotCount) {
            return ItemStack.EMPTY;
        }
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return ItemStack.EMPTY;
        }
        var root = customData.copyTag();
        if (!root.contains(calibrationKey, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        var adjustments = root.getCompound(calibrationKey).getList(ADJUSTMENTS_TAG, Tag.TAG_COMPOUND);
        for (var value : adjustments) {
            var entry = (CompoundTag) value;
            if (entry.getInt(SLOT_TAG) != slot) {
                continue;
            }
            var itemId = ResourceLocation.tryParse(entry.getString(ITEM_TAG));
            if (itemId == null) {
                return ItemStack.EMPTY;
            }
            var item = BuiltInRegistries.ITEM.get(itemId);
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }

    static void set(ItemStack stack, String calibrationKey, int slot, int slotCount, ItemStack adjustment) {
        if (stack.isEmpty() || slot < 0 || slot >= slotCount) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            var calibration = root.contains(calibrationKey, Tag.TAG_COMPOUND)
                    ? root.getCompound(calibrationKey)
                    : new CompoundTag();
            var current = calibration.getList(ADJUSTMENTS_TAG, Tag.TAG_COMPOUND);
            var replacement = new ListTag();
            for (var value : current) {
                var entry = (CompoundTag) value;
                if (entry.getInt(SLOT_TAG) != slot) {
                    replacement.add(entry.copy());
                }
            }
            if (!adjustment.isEmpty()) {
                var itemId = BuiltInRegistries.ITEM.getKey(adjustment.getItem());
                var entry = new CompoundTag();
                entry.putInt(SLOT_TAG, slot);
                entry.putString(ITEM_TAG, itemId.toString());
                replacement.add(entry);
            }
            if (replacement.isEmpty()) {
                root.remove(calibrationKey);
            } else {
                calibration.put(ADJUSTMENTS_TAG, replacement);
                root.put(calibrationKey, calibration);
            }
        });
    }
}
