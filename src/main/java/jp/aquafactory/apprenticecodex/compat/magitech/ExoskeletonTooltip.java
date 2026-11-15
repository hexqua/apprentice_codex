package jp.aquafactory.apprenticecodex.compat.magitech;

import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonConfigState;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.MagiEngineersLegExoskeleton;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.stln.magitech.content.event.KeyMappingEvent;
import net.stln.magitech.feature.element.Element;

import java.util.List;

public final class ExoskeletonTooltip {
    private ExoskeletonTooltip() {}

    public static void append(List<Component> lines) {
        append(lines, KeyMappingEvent.DOUBLE_JUMP.get().getTranslatedKeyMessage(),
                KeyMappingEvent.LONG_JUMP.get().getTranslatedKeyMessage());
    }

    public static void append(List<Component> lines, Component lifterKey, Component striderKey) {
        var lifter = Component.translatable("item.magitech.aether_lifter").withColor(Element.HOLLOW.getTextColor().getRGB());
        var strider = Component.translatable("item.magitech.flamglide_strider").withColor(Element.EMBER.getTextColor().getRGB());
        line(lines, 1, lifterKey, lifter);
        line(lines, 2, Component.literal(Integer.toString(ExoskeletonConfigState.lifterManaCost())).withStyle(ChatFormatting.AQUA));
        line(lines, 3, striderKey, strider);
        line(lines, 4, Component.literal(Integer.toString(ExoskeletonConfigState.striderManaCost())).withStyle(ChatFormatting.AQUA));
        line(lines, 5, strider);
    }

    private static void line(List<Component> lines, int number, Object... args) {
        lines.add(Component.literal(" ").append(Component.translatable(
                MagiEngineersLegExoskeleton.KEY + ".desc_" + number, args)).withStyle(ChatFormatting.YELLOW));
    }
}
