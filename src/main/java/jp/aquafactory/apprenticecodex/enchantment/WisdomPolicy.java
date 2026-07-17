package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.item.Item;

/**
 * Wisdom の通常付与面と装備場所ごとの効果適用をアイテム側で宣言するポリシー。
 */
public interface WisdomPolicy {
    default boolean isWisdomActiveWhileHeld() {
        return true;
    }

    default boolean supportsDirectWisdomApplication() {
        return true;
    }

    static boolean supportsDirectApplication(Item item) {
        return item instanceof WisdomPolicy policy
                && policy.supportsDirectWisdomApplication();
    }
}
