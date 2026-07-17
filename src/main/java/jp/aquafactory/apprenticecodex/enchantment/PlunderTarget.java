package jp.aquafactory.apprenticecodex.enchantment;

import net.minecraft.world.item.Item;

/**
 * Plunder の通常付与と手持ち効果へ参加するアイテムを示すマーカー。
 */
public interface PlunderTarget {
    static boolean supportsDirectApplication(Item item) {
        return item instanceof PlunderTarget;
    }
}
