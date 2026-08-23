package jp.aquafactory.apprenticecodex.item.armor;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

/** エンドゲーム防具の追加スクロールを、調整アイテムとは独立して保存する。 */
final class EndgameArmorScrollStorage {
    static final String ROOT_TAG = "ApprenticeCodexEndgameArmorScroll";
    private static final String ITEM_TAG = "Item";
    private static final HolderLookup.Provider FALLBACK_SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private EndgameArmorScrollStorage() {
    }

    static @NotNull ItemStack get(@NotNull ItemStack armorStack) {
        return get(armorStack, serializationLookup());
    }

    static @NotNull ItemStack get(
            @NotNull ItemStack armorStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (armorStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var customData = armorStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return ItemStack.EMPTY;
        }
        var root = customData.copyTag();
        if (!root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        var stored = root.getCompound(ROOT_TAG);
        if (!stored.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return normalize(ItemStack.parseOptional(lookupProvider, stored.getCompound(ITEM_TAG)));
    }

    static void set(@NotNull ItemStack armorStack, @NotNull ItemStack scrollStack) {
        set(armorStack, scrollStack, serializationLookup());
    }

    static void set(
            @NotNull ItemStack armorStack,
            @NotNull ItemStack scrollStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (armorStack.isEmpty()) {
            return;
        }
        var normalized = normalize(scrollStack);
        CustomData.update(DataComponents.CUSTOM_DATA, armorStack, root -> {
            if (normalized.isEmpty()) {
                root.remove(ROOT_TAG);
                return;
            }
            var stored = new net.minecraft.nbt.CompoundTag();
            stored.put(ITEM_TAG, normalized.saveOptional(lookupProvider));
            root.put(ROOT_TAG, stored);
        });
    }

    private static @NotNull ItemStack normalize(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    private static HolderLookup.Provider serializationLookup() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? FALLBACK_SERIALIZATION_LOOKUP : server.registryAccess();
    }
}
