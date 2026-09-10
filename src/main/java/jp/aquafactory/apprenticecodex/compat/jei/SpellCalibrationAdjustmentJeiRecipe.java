package jp.aquafactory.apprenticecodex.compat.jei;

import jp.aquafactory.apprenticecodex.item.CalibrationConstraintDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record SpellCalibrationAdjustmentJeiRecipe(
        ResourceLocation id,
        ItemStack target,
        List<ItemStack> adjustments,
        List<ItemStack> results,
        java.util.function.Supplier<List<Component>> effectLinesSupplier,
        CalibrationConstraintDisplay constraintDisplay
) {
    public SpellCalibrationAdjustmentJeiRecipe {
        target = target.copyWithCount(1);
        adjustments = adjustments.stream().map(stack -> stack.copyWithCount(1)).toList();
        results = results.stream().map(stack -> stack.copyWithCount(1)).toList();
        if (adjustments.size() != results.size()) {
            throw new IllegalArgumentException("Calibration adjustment inputs and results must have the same size.");
        }
    }

    public List<Component> effectLines() {
        // サーバー設定の同期・再読込後も、JEI登録時の倍率を表示し続けない。
        return List.copyOf(effectLinesSupplier.get());
    }

    public SpellCalibrationAdjustmentJeiRecipe(ResourceLocation id, ItemStack target, List<ItemStack> adjustments,
                                              List<ItemStack> results, List<Component> effectLines,
                                              CalibrationConstraintDisplay constraintDisplay) {
        this(id, target, adjustments, results, () -> List.copyOf(effectLines), constraintDisplay);
    }
}
