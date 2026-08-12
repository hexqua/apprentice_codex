package jp.aquafactory.apprenticecodex.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** 術式調整台の調整アイテムを、対象 Item に依存しない共通形式で保存する。 */
public final class CalibrationAdjustmentStorage {
    static final String ROOT_TAG = "ApprenticeCodexCalibrationAdjustments";
    private static final String SLOTS_TAG = "Slots";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";

    private CalibrationAdjustmentStorage() {
    }

    public static @NotNull ItemStack get(@NotNull ItemStack owner, int slot, int slotCount) {
        if (!isValidAccess(owner, slot, slotCount)) {
            return ItemStack.EMPTY;
        }
        var root = owner.getTag();
        if (root != null && root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return readCanonical(root.getCompound(ROOT_TAG), slot);
        }
        return normalize(LegacyCalibrationAdjustmentStorage.readLegacy(owner, slot));
    }

    public static void set(
            @NotNull ItemStack owner,
            int slot,
            int slotCount,
            @NotNull ItemStack adjustment
    ) {
        if (!isValidAccess(owner, slot, slotCount)) {
            return;
        }
        var items = readAllForWrite(owner, slotCount);
        items.set(slot, normalize(adjustment));
        writeCanonical(owner, items);
        LegacyCalibrationAdjustmentStorage.removeLegacy(owner);
    }

    private static @NotNull List<ItemStack> readAllForWrite(ItemStack owner, int slotCount) {
        var items = new ArrayList<ItemStack>(slotCount);
        var root = owner.getTag();
        var hasCanonicalData = root != null && root.contains(ROOT_TAG, Tag.TAG_COMPOUND);
        var canonical = hasCanonicalData ? root.getCompound(ROOT_TAG) : null;
        for (var slot = 0; slot < slotCount; ++slot) {
            var item = hasCanonicalData
                    ? readCanonical(canonical, slot)
                    : LegacyCalibrationAdjustmentStorage.readLegacy(owner, slot);
            items.add(normalize(item));
        }
        return items;
    }

    private static @NotNull ItemStack readCanonical(CompoundTag canonical, int slot) {
        if (!canonical.contains(SLOTS_TAG, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }
        var slots = canonical.getList(SLOTS_TAG, Tag.TAG_COMPOUND);
        for (var index = 0; index < slots.size(); ++index) {
            var entry = slots.getCompound(index);
            if (entry.getInt(SLOT_TAG) == slot && entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
                return normalize(ItemStack.of(entry.getCompound(ITEM_TAG)));
            }
        }
        return ItemStack.EMPTY;
    }

    private static void writeCanonical(ItemStack owner, List<ItemStack> items) {
        var slots = new ListTag();
        for (var slot = 0; slot < items.size(); ++slot) {
            var item = items.get(slot);
            if (item.isEmpty()) {
                continue;
            }
            var entry = new CompoundTag();
            entry.putInt(SLOT_TAG, slot);
            entry.put(ITEM_TAG, item.save(new CompoundTag()));
            slots.add(entry);
        }
        if (slots.isEmpty()) {
            owner.removeTagKey(ROOT_TAG);
            return;
        }
        var canonical = new CompoundTag();
        canonical.put(SLOTS_TAG, slots);
        owner.getOrCreateTag().put(ROOT_TAG, canonical);
    }

    private static @NotNull ItemStack normalize(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var normalized = stack.copy();
        normalized.setCount(1);
        return normalized;
    }

    private static boolean isValidAccess(ItemStack owner, int slot, int slotCount) {
        return !owner.isEmpty() && slot >= 0 && slot < slotCount;
    }
}
