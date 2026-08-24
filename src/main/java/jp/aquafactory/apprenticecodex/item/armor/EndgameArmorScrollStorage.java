package jp.aquafactory.apprenticecodex.item.armor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** エンドゲーム防具の追加スクロールを、調整アイテムとは独立して保存する。 */
final class EndgameArmorScrollStorage {
    static final String ROOT_TAG = "ApprenticeCodexEndgameArmorScroll";
    private static final String ITEM_TAG = "Item";
    private EndgameArmorScrollStorage() {
    }

    static @NotNull ItemStack get(@NotNull ItemStack armorStack) {
        if (armorStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        var root = armorStack.getTag();
        if (root == null || !root.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        var stored = root.getCompound(ROOT_TAG);
        if (!stored.contains(ITEM_TAG, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        return normalize(ItemStack.of(stored.getCompound(ITEM_TAG)));
    }

    static void set(@NotNull ItemStack armorStack, @NotNull ItemStack scrollStack) {
        if (armorStack.isEmpty()) {
            return;
        }
        var normalized = normalize(scrollStack);
        if (normalized.isEmpty()) {
            armorStack.removeTagKey(ROOT_TAG);
            return;
        }
        var stored = new CompoundTag();
        stored.put(ITEM_TAG, normalized.save(new CompoundTag()));
        armorStack.getOrCreateTag().put(ROOT_TAG, stored);
    }

    private static @NotNull ItemStack normalize(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }
}
