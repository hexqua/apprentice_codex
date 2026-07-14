package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.item.SneakSelectionUiItem;
import net.minecraft.world.entity.player.Player;

public final class SneakSelectionUiHandResolver {
    private SneakSelectionUiHandResolver() {
    }

    /**
     * スニーク開始時にオフハンド側の選択 UI を抑制するかを判定する。
     * 表示中の持ち替えでは再評価せず、途中から別の選択 UI を開かない既存仕様を維持する。
     */
    public static boolean shouldSuppressOffhandSelection(Player player) {
        return player.getMainHandItem().getItem() instanceof SneakSelectionUiItem;
    }
}
