package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.InteractionHand;

import java.util.List;

/**
 * 即時選択UIのカーソル状態。ItemStackへの反映や通信を持たず、入力ごとの状態遷移だけを扱う。
 */
public record SneakSelectionState(
        InteractionHand hand,
        List<SneakSelectionView> views,
        int selectedViewIndex
) {
    public SneakSelectionState {
        views = List.copyOf(views);
        if (views.isEmpty() || selectedViewIndex < 0 || selectedViewIndex >= views.size()) {
            throw new IllegalArgumentException("Selection state requires a valid view index");
        }
    }

    public static SneakSelectionState open(
            InteractionHand hand,
            List<SneakSelectionView> views,
            int itemSelectionIndex
    ) {
        return new SneakSelectionState(hand, views, resolveViewIndex(views, itemSelectionIndex));
    }

    public SneakSelectionState refresh(List<SneakSelectionView> refreshedViews, int itemSelectionIndex) {
        return new SneakSelectionState(hand, refreshedViews, resolveViewIndex(refreshedViews, itemSelectionIndex));
    }

    public SneakSelectionState move(int direction) {
        if (direction == 0 || selectableViewCount() <= 1) {
            return this;
        }
        for (var offset = 1; offset <= views.size(); ++offset) {
            var nextIndex = Math.floorMod(selectedViewIndex + direction * offset, views.size());
            if (views.get(nextIndex).selectable()) {
                return nextIndex == selectedViewIndex
                        ? this
                        : new SneakSelectionState(hand, views, nextIndex);
            }
        }
        return this;
    }

    public SneakSelectionView selectedView() {
        return views.get(selectedViewIndex);
    }

    public int selectedItemIndex() {
        return selectedView().selectionIndex();
    }

    public int selectableViewCount() {
        var count = 0;
        for (var view : views) {
            if (view.selectable()) {
                ++count;
            }
        }
        return count;
    }

    private static int resolveViewIndex(List<SneakSelectionView> views, int itemSelectionIndex) {
        if (views.isEmpty()) {
            throw new IllegalArgumentException("Selection state requires at least one view");
        }
        for (var index = 0; index < views.size(); ++index) {
            var view = views.get(index);
            if (view.selectionIndex() == itemSelectionIndex && view.selectable()) {
                return index;
            }
        }
        for (var index = 0; index < views.size(); ++index) {
            if (views.get(index).selectable()) {
                return index;
            }
        }
        return 0;
    }
}
