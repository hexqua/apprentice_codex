package jp.aquafactory.apprenticecodex.gametest;

import jp.aquafactory.apprenticecodex.item.SpellCalibrationAdjustmentTarget;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

final class SpellCalibrationAdjustmentGameTestSupport {
    private SpellCalibrationAdjustmentGameTestSupport() {
    }

    static @NotNull ItemStack getCalibrationAdjustment(@NotNull ItemStack targetStack, int slot) {
        return getTarget(targetStack).getCalibrationAdjustment(targetStack, slot);
    }

    static boolean setCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack adjustment
    ) {
        return getTarget(targetStack).trySetCalibrationAdjustment(targetStack, slot, adjustment);
    }

    static boolean canPlaceCalibrationAdjustment(
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack adjustment
    ) {
        return getTarget(targetStack).canPlaceCalibrationAdjustment(targetStack, slot, adjustment);
    }

    private static SpellCalibrationAdjustmentTarget getTarget(@NotNull ItemStack targetStack) {
        if (targetStack.getItem() instanceof SpellCalibrationAdjustmentTarget target) {
            return target;
        }
        throw new IllegalArgumentException("ItemStack is not a spell calibration adjustment target");
    }
}
