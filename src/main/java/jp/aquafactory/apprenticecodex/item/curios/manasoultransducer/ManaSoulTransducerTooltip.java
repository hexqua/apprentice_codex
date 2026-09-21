package jp.aquafactory.apprenticecodex.item.curios.manasoultransducer;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.compat.malum.MalumStaffChargeBridge;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
final class ManaSoulTransducerTooltip {
    private ManaSoulTransducerTooltip() {}

    static void appendStatus(List<Component> result, String id) {
        var player = Minecraft.getInstance().player;
        if (player == null || !MalumStaffChargeBridge.isAvailable()) return;
        double duration = ManaSoulTransducerLogic.durationModifier(player.getAttributeValue(AttributeRegistry.CAST_TIME_REDUCTION.get()), ManaSoulTransducerConfigState.castRate());
        result.add(Component.translatable(id + ".status.duration", percent(duration)).withStyle(ChatFormatting.GRAY));
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%+.1f", value == 0 ? 0D : value * 100D);
    }
}
