package jp.aquafactory.apprenticecodex.item.swingstaff;

import jp.aquafactory.apprenticecodex.compat.malum.MalumMnemonicBladeBridge;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SoulstainedSteelSwingcastStaffClientManaCost {
    private SoulstainedSteelSwingcastStaffClientManaCost() {
    }

    public static double resolveFullBurstManaCost() {
        var baseManaCost = SoulstainedSteelSwingcastStaffConfigState.manaCostPerBlade();
        if (baseManaCost <= 0.0D) {
            return 0.0D;
        }

        var player = Minecraft.getInstance().player;
        // タイトル画面などplayerが未生成の場面では、外部MODの属性取得へnullを渡さず基準値を使う。
        var chargeRecoveryRate = player == null ? 1.0D : MalumMnemonicBladeBridge.getChargeRecoveryRate(player);
        return SoulstainedSteelSwingcastStaff.resolveManaCost(baseManaCost, chargeRecoveryRate)
                * SoulstainedSteelSwingcastStaff.MAX_BLADE_COUNT;
    }

    public static long resolveDisplayedFullBurstManaCost() {
        return (long) Math.ceil(resolveFullBurstManaCost() - 1.0e-9D);
    }
}
