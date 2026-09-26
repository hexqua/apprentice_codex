package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.item.Item;

/** 自身のスクロールスロットに Transcendence を適用できるアイテム。 */
public interface TranscendenceTarget {
    static boolean supportsDirectApplication(Item item) {
        return item instanceof TranscendenceTarget;
    }
}
