package jp.aquafactory.apprenticecodex.item.swingstaff;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class SoulstainedSteelSwingcastStaffClientTooltip {
    private SoulstainedSteelSwingcastStaffClientTooltip() {
    }

    public static @Nullable Component createLine() {
        var displayedManaCost = SoulstainedSteelSwingcastStaffClientManaCost.resolveDisplayedFullBurstManaCost();
        if (displayedManaCost <= 0L) {
            return null;
        }
        return Component.translatable(
                "item.apprenticecodex.soulstained_steel_swingcast_staff.mana_cost",
                displayedManaCost
        ).withStyle(ChatFormatting.AQUA);
    }
}
