package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 術式調整台のスクロールを通常の SpellContainer ではなく独自領域へ保存する対象。
 */
public interface StoredSpellCalibrationImbueTarget extends SpellCalibrationImbueTarget {
    boolean hasAnyStoredCalibrationScroll(@NotNull ItemStack targetStack);
}
