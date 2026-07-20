package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.item.ItemStack;

/**
 * スニーク開始時に選択 UI を表示するアイテムを示す。
 * 両手に該当アイテムがある場合は、メインハンド側の選択 UI を優先する。
 */
public interface SneakSelectionUiItem {
    default boolean isSneakSelectionUiEnabled(ItemStack stack) {
        return true;
    }
}
