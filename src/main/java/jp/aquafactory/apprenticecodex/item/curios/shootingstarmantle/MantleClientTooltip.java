package jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle;

import jp.aquafactory.apprenticecodex.item.ScrollSlotTooltipClientHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class MantleClientTooltip {
    private MantleClientTooltip() { }

    public static void append(ItemStack stack, List<Component> lines) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var data = MantleCalibration.tooltipData(stack, MantleCalibration.serializationLookup());
        if (data.entries().isEmpty()) return;
        lines.add(Component.empty());
        // 外套に選択状態はなく、有効な全スクロールを常時ホイールへ公開する。
        ScrollSlotTooltipClientHelper.appendList(lines, data, player);
    }
}
