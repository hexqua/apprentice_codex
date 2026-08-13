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
        List<Component> effectLines,
        CalibrationConstraintDisplay constraintDisplay
) {
    public SpellCalibrationAdjustmentJeiRecipe {
        target = target.copyWithCount(1);
        adjustments = adjustments.stream().map(stack -> stack.copyWithCount(1)).toList();
        effectLines = List.copyOf(effectLines);
    }
}
