package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** 術式調整台の調整スロットを持つ Item。 */
public interface SpellCalibrationAdjustmentTarget {
    int getCalibrationAdjustmentSlotCount(@NotNull ItemStack targetStack);

    default @NotNull ItemStack getCalibrationAdjustment(@NotNull ItemStack targetStack, int slot) {
        if (targetStack.isEmpty() || targetStack.getItem() != this) {
            return ItemStack.EMPTY;
        }
        return CalibrationAdjustmentStorage.get(
                targetStack,
                slot,
                getCalibrationAdjustmentSlotCount(targetStack)
        );
    }

    /**
     * 新しい候補は profile の論理制約を通して保存する。空スタックは旧データの取り外しを保証するため常に許可する。
     */
    default boolean trySetCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack adjustment
    ) {
        if (!canPlaceCalibrationAdjustment(targetStack, slot, adjustment)) {
            return false;
        }
        CalibrationAdjustmentStorage.set(
                targetStack,
                slot,
                getCalibrationAdjustmentSlotCount(targetStack),
                adjustment
        );
        onCalibrationAdjustmentsChanged(targetStack);
        return true;
    }

    /** 調整内容から派生して保存する属性や選択状態がある Item だけが上書きする。 */
    default void onCalibrationAdjustmentsChanged(@NotNull ItemStack targetStack) {
    }

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
