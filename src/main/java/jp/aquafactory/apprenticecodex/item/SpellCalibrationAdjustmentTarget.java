package jp.aquafactory.apprenticecodex.item;

import net.minecraft.core.HolderLookup;
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
     * 動的レジストリを含む ItemStack を扱う対象は、接続中 Level の lookup provider を使って復元する。
     */
    default @NotNull ItemStack getCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (targetStack.isEmpty() || targetStack.getItem() != this) {
            return ItemStack.EMPTY;
        }
        return CalibrationAdjustmentStorage.get(
                targetStack,
                slot,
                getCalibrationAdjustmentSlotCount(targetStack),
                lookupProvider
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
        return trySetCalibrationAdjustment(
                targetStack,
                slot,
                adjustment,
                CalibrationAdjustmentStorage.serializationLookup()
        );
    }

    /**
     * 動的レジストリを含む ItemStack を扱う対象は、接続中 Level の lookup provider を使って保存する。
     */
    default boolean trySetCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack adjustment,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (!canPlaceCalibrationAdjustment(targetStack, slot, adjustment, lookupProvider)) {
            return false;
        }
        CalibrationAdjustmentStorage.set(
                targetStack,
                slot,
                getCalibrationAdjustmentSlotCount(targetStack),
                adjustment,
                lookupProvider
        );
        onCalibrationAdjustmentsChanged(targetStack, lookupProvider);
        return true;
    }

    /** 調整内容から派生して保存する属性や選択状態がある Item だけが上書きする。 */
    default void onCalibrationAdjustmentsChanged(
            @NotNull ItemStack targetStack,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
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

    default boolean canPlaceCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack adjustment,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        if (targetStack.isEmpty() || targetStack.getItem() != this) {
            return false;
        }
        return getCalibrationAdjustmentProfile(targetStack)
                .canPlace(this, targetStack, slot, adjustment, lookupProvider);
    }
}
