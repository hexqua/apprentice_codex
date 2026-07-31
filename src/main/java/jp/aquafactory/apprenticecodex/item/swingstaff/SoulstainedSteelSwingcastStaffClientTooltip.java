package jp.aquafactory.apprenticecodex.item.swingstaff;

import jp.aquafactory.apprenticecodex.compat.malum.MalumMnemonicBladeBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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

        var player = Minecraft.getInstance().player;
        // タイトル画面などplayerが未生成の場面では、外部MODの属性取得へnullを渡さず基準値で表示する。
        var chargeRecoveryRate = player == null ? 1.0D : MalumMnemonicBladeBridge.getChargeRecoveryRate(player);
        var displayedManaCost = SoulstainedSteelSwingcastStaff.resolveDisplayedTotalManaCost(
                baseManaCost,
                chargeRecoveryRate
        );
        return Component.translatable(
                "item.apprenticecodex.soulstained_steel_swingcast_staff.mana_cost",
                displayedManaCost
        ).withStyle(ChatFormatting.AQUA);
    }
}
