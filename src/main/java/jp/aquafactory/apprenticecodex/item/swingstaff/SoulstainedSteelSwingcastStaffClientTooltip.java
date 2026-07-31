package jp.aquafactory.apprenticecodex.item.swingstaff;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class SoulstainedSteelSwingcastStaffClientTooltip {
    private SoulstainedSteelSwingcastStaffClientTooltip() {
    }

    public static @Nullable Component createLine() {
        var baseManaCost = SoulstainedSteelSwingcastStaffConfigState.manaCostPerBlade();
        if (baseManaCost <= 0.0D) {
            return null;
        }

        var displayedManaCost = SoulstainedSteelSwingcastStaff.resolveDisplayedTotalManaCost(baseManaCost);
        return Component.translatable(
                "item.apprenticecodex.soulstained_steel_swingcast_staff.mana_cost",
                displayedManaCost
        ).withStyle(ChatFormatting.AQUA);
    }
}
