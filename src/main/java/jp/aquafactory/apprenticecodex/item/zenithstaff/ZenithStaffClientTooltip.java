package jp.aquafactory.apprenticecodex.item.zenithstaff;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ZenithStaffClientTooltip {
    private ZenithStaffClientTooltip() {
    }

    public static List<Component> createLines() {
        var player = Minecraft.getInstance().player;
        var snapshot = ZenithStaffPowerHelper.resolvePowerSnapshot(player);
        if (!snapshot.hasSchoolBonus()) {
            return List.of(Component.translatable("item.apprenticecodex.zenith_staff.no_bonus")
                    .withStyle(ChatFormatting.GRAY));
        }

        return List.of(
                Component.translatable(
                        "item.apprenticecodex.zenith_staff.desc_1",
                        snapshot.bonusPercent()
                ).withStyle(ChatFormatting.GRAY),
                Component.translatable(
                        "item.apprenticecodex.zenith_staff.desc_2",
                        createSchoolList(snapshot),
                        manaCostMultiplierPercent()
                ).withStyle(ChatFormatting.GRAY)
        );
    }

    private static Component createSchoolList(ZenithStaffPowerHelper.PowerSnapshot snapshot) {
        MutableComponent result = Component.empty();
        for (var index = 0; index < snapshot.strongestSchools().size(); ++index) {
            if (index > 0) {
                result.append(",");
            }
            result.append(snapshot.strongestSchools().get(index).getDisplayName());
        }
        return result;
    }

    private static int manaCostMultiplierPercent() {
        return Math.round(ZenithStaffConfigState.manaCostMultiplier() * 100.0F);
    }
}
