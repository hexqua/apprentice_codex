package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 術式調整台のスクロールを通常の SpellContainer ではなく独自領域へ保存する対象。
 */
public interface StoredSpellCalibrationImbueTarget extends SpellCalibrationImbueTarget {
    /**
     * 同じ Item クラス内で SpellContainer 型と独自保存型を部位などにより使い分ける場合の判定。
     */
    default boolean usesStoredCalibrationScrolls(@NotNull ItemStack targetStack) {
        return true;
    }

    boolean hasAnyStoredCalibrationScroll(@NotNull ItemStack targetStack);
}
