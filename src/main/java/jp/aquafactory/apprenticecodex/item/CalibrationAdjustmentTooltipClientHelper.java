package jp.aquafactory.apprenticecodex.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/** client に接続中の Level が持つ動的レジストリを tooltip data の復元へ渡す。 */
final class CalibrationAdjustmentTooltipClientHelper {
    private CalibrationAdjustmentTooltipClientHelper() {
    }

    static @NotNull CalibrationAdjustmentTooltip create(
            @NotNull ItemStack targetStack,
            @NotNull SpellCalibrationAdjustmentTarget target
    ) {
        var level = Minecraft.getInstance().level;
        return level == null
                ? CalibrationAdjustmentTooltip.create(targetStack, target)
                : CalibrationAdjustmentTooltip.create(targetStack, target, level.registryAccess());
    }
}
