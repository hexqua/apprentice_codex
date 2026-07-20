package jp.aquafactory.apprenticecodex.item.chargecastcatalystbook;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.world.InteractionHand;

import java.util.List;

/**
 * 即時選択UIのカーソル状態。ItemStackへの反映や通信を持たないため、入力ごとの状態遷移を単独で検証できる。
 */
public record ChargecastCatalystbookSelectionState(
        InteractionHand hand,
        List<ChargecastCatalystbook.ScrollSelectionView> views,
        int selectedViewIndex
) {
    public ChargecastCatalystbookSelectionState {
        views = List.copyOf(views);
        if (views.isEmpty() || selectedViewIndex < 0 || selectedViewIndex >= views.size()) {
            throw new IllegalArgumentException("Selection state requires a valid view index");
        }
    }

    public static ChargecastCatalystbookSelectionState open(
            InteractionHand hand,
            List<ChargecastCatalystbook.ScrollSelectionView> views
    ) {
        return new ChargecastCatalystbookSelectionState(hand, views, findInitialViewIndex(views));
    }

    public ChargecastCatalystbookSelectionState refresh(
            List<ChargecastCatalystbook.ScrollSelectionView> refreshedViews,
            int itemSelectedScrollIndex
    ) {
        var refreshedIndex = indexOfScroll(refreshedViews, itemSelectedScrollIndex);
        if (refreshedIndex < 0) {
            refreshedIndex = findInitialViewIndex(refreshedViews);
        }
        return new ChargecastCatalystbookSelectionState(hand, refreshedViews, refreshedIndex);
    }

    public ChargecastCatalystbookSelectionState move(int direction) {
        if (direction == 0 || selectableViewCount() <= 1) {
            return this;
        }
        for (var offset = 1; offset <= views.size(); ++offset) {
            var nextIndex = Math.floorMod(selectedViewIndex + direction * offset, views.size());
            if (hasSpell(views.get(nextIndex))) {
                return nextIndex == selectedViewIndex
                        ? this
                        : new ChargecastCatalystbookSelectionState(hand, views, nextIndex);
            }
        }
        return this;
    }

    public ChargecastCatalystbook.ScrollSelectionView selectedView() {
        return views.get(selectedViewIndex);
    }

    public int selectedScrollIndex() {
        return selectedView().scrollIndex();
    }

    public int selectableViewCount() {
        var count = 0;
        for (var view : views) {
            if (hasSpell(view)) {
                ++count;
            }
        }
        return count;
    }

    public static boolean hasSpell(ChargecastCatalystbook.ScrollSelectionView view) {
        return view.spellData() != SpellData.EMPTY && view.spellData().getSpell() != null
                && view.spellData().getSpell() != SpellRegistry.none();
    }

    private static int findInitialViewIndex(List<ChargecastCatalystbook.ScrollSelectionView> views) {
        if (views.isEmpty()) {
            throw new IllegalArgumentException("Selection state requires at least one view");
        }
        for (var index = 0; index < views.size(); ++index) {
            if (views.get(index).selected() && hasSpell(views.get(index))) {
                return index;
            }
        }
        for (var index = 0; index < views.size(); ++index) {
            if (hasSpell(views.get(index))) {
                return index;
            }
        }
        return 0;
    }

    private static int indexOfScroll(
            List<ChargecastCatalystbook.ScrollSelectionView> views,
            int scrollIndex
    ) {
        for (var index = 0; index < views.size(); ++index) {
            if (views.get(index).scrollIndex() == scrollIndex && hasSpell(views.get(index))) {
                return index;
            }
        }
        return -1;
    }
}
