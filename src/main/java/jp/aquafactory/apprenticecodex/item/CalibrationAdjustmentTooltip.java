package jp.aquafactory.apprenticecodex.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** 術式調整台の調整スロットを、空きスロットを含む固定順で表示する tooltip data。 */
public record CalibrationAdjustmentTooltip(NonNullList<ItemStack> items) implements TooltipComponent {
    public static @NotNull CalibrationAdjustmentTooltip create(
            @NotNull ItemStack targetStack,
            @NotNull SpellCalibrationAdjustmentTarget target
    ) {
        return create(targetStack, target, CalibrationAdjustmentStorage.serializationLookup());
    }

    public static @NotNull CalibrationAdjustmentTooltip create(
            @NotNull ItemStack targetStack,
            @NotNull SpellCalibrationAdjustmentTarget target,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        var slotCount = target.getCalibrationAdjustmentSlotCount(targetStack);
        var items = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        for (var slot = 0; slot < slotCount; ++slot) {
            items.set(slot, target.getCalibrationAdjustment(targetStack, slot, lookupProvider));
        }
        return new CalibrationAdjustmentTooltip(items);
    }
}
