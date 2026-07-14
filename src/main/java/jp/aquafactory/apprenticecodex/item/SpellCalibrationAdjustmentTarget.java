package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** 術式調整台の調整スロットを持つ Item。 */
public interface SpellCalibrationAdjustmentTarget {
    int getCalibrationAdjustmentSlotCount(@NotNull ItemStack targetStack);

    @NotNull
    ItemStack getCalibrationAdjustment(@NotNull ItemStack targetStack, int slot);

    /**
     * 新しい候補は profile の論理制約を通して保存する。空スタックは旧データの取り外しを保証するため常に許可する。
     */
    boolean trySetCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack adjustment
    );

    @NotNull
    CalibrationAdjustmentProfile getCalibrationAdjustmentProfile(@NotNull ItemStack targetStack);

    default boolean canPlaceCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack adjustment
    ) {
        if (targetStack.isEmpty() || targetStack.getItem() != this) {
            return false;
        }
        return getCalibrationAdjustmentProfile(targetStack).canPlace(this, targetStack, slot, adjustment);
    }
}
