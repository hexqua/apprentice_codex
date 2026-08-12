package jp.aquafactory.apprenticecodex.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** 術式調整台の調整アイテムを、対象 Item に依存しない共通形式で保存する。 */
public final class CalibrationAdjustmentStorage {
    static final String ROOT_TAG = "ApprenticeCodexCalibrationAdjustments";
    private static final String SLOTS_TAG = "Slots";
    private static final String SLOT_TAG = "Slot";
    private static final String ITEM_TAG = "Item";
    private static final HolderLookup.Provider FALLBACK_SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private CalibrationAdjustmentStorage() {
    }

    public static @NotNull ItemStack get(@NotNull ItemStack owner, int slot, int slotCount) {
        return get(owner, slot, slotCount, serializationLookup());
    }

    public static void set(
            @NotNull ItemStack owner,
            int slot,
            int slotCount,
            @NotNull ItemStack adjustment
    ) {
        set(owner, slot, slotCount, adjustment, serializationLookup());
    }

    public static @NotNull ItemStack get(
            @NotNull ItemStack owner,
            int slot,
            int slotCount,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidAccess(owner, slot, slotCount)) {
            return ItemStack.EMPTY;
        }
        var root = getCustomDataTag(owner);
        if (root != null && root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return readCanonical(root.getCompound(ROOT_TAG), slot, lookupProvider);
        }
        return normalize(LegacyCalibrationAdjustmentStorage.readLegacy(owner, slot, lookupProvider));
    }

    public static void set(
            @NotNull ItemStack owner,
            int slot,
            int slotCount,
            @NotNull ItemStack adjustment,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!isValidAccess(owner, slot, slotCount)) {
            return;
        }
        var items = readAllForWrite(owner, slotCount, lookupProvider);
        items.set(slot, normalize(adjustment));
        writeCanonical(owner, items, lookupProvider);
        LegacyCalibrationAdjustmentStorage.removeLegacy(owner);
    }

    private static @NotNull List<ItemStack> readAllForWrite(
            ItemStack owner,
            int slotCount,
            HolderLookup.Provider lookupProvider
    ) {
        var items = new ArrayList<ItemStack>(slotCount);
        var root = getCustomDataTag(owner);
        var hasCanonicalData = root != null && root.contains(ROOT_TAG, Tag.TAG_COMPOUND);
        var canonical = hasCanonicalData ? root.getCompound(ROOT_TAG) : null;
        for (var slot = 0; slot < slotCount; ++slot) {
            var item = hasCanonicalData
                    ? readCanonical(canonical, slot, lookupProvider)
                    : LegacyCalibrationAdjustmentStorage.readLegacy(owner, slot, lookupProvider);
            items.add(normalize(item));
        }
        return items;
    }

    private static @NotNull ItemStack readCanonical(
            CompoundTag canonical,
            int slot,
            HolderLookup.Provider lookupProvider
    ) {
        if (!canonical.contains(SLOTS_TAG, Tag.TAG_LIST)) {
            return ItemStack.EMPTY;
        }
        var slots = canonical.getList(SLOTS_TAG, Tag.TAG_COMPOUND);
        for (var index = 0; index < slots.size(); ++index) {
            var entry = slots.getCompound(index);
            if (entry.getInt(SLOT_TAG) == slot && entry.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
                var stored = entry.getCompound(ITEM_TAG);
                var parsed = normalize(ItemStack.parseOptional(lookupProvider, stored));
                return parsed.isEmpty() ? createIdFallback(stored) : parsed;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void writeCanonical(
            ItemStack owner,
            List<ItemStack> items,
            HolderLookup.Provider lookupProvider
    ) {
        CustomData.update(DataComponents.CUSTOM_DATA, owner, root -> {
            var slots = new ListTag();
            for (var slot = 0; slot < items.size(); ++slot) {
                var item = items.get(slot);
                if (item.isEmpty()) {
                    continue;
                }
                var entry = new CompoundTag();
                entry.putInt(SLOT_TAG, slot);
                entry.put(ITEM_TAG, item.saveOptional(lookupProvider));
                slots.add(entry);
            }
            if (slots.isEmpty()) {
                root.remove(ROOT_TAG);
                return;
            }
            var canonical = new CompoundTag();
            canonical.put(SLOTS_TAG, slots);
            root.put(ROOT_TAG, canonical);
        });
    }

    private static @NotNull ItemStack normalize(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    private static @NotNull ItemStack createIdFallback(CompoundTag storedStack) {
        var id = ResourceLocation.tryParse(storedStack.getString("id"));
        if (id == null) {
            return ItemStack.EMPTY;
        }
        var item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static boolean isValidAccess(ItemStack owner, int slot, int slotCount) {
        return !owner.isEmpty() && slot >= 0 && slot < slotCount;
    }

    private static CompoundTag getCustomDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }

    static HolderLookup.Provider serializationLookup() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? FALLBACK_SERIALIZATION_LOOKUP : server.registryAccess();
    }
}
