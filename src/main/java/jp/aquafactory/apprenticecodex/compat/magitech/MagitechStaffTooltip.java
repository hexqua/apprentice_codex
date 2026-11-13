package jp.aquafactory.apprenticecodex.compat.magitech;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.item.magiengineersstaff.MagiEngineersStaff;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.stln.magitech.feature.element.Element;
import net.stln.magitech.feature.tool.property.ToolPropertyCategory;

import java.util.List;

public final class MagitechStaffTooltip {
    private MagitechStaffTooltip() {
    }

    public static void append(List<Component> lines) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (Screen.hasShiftDown() || Screen.hasAltDown()) {
            lines.add(Component.translatable(MagiEngineersStaff.KEY + ".detail.desc_1").withStyle(ChatFormatting.GOLD));
            for (Element element : MagiEngineersStaffCompat.ELEMENTS) {
                if (element == Element.LOGOS) continue;
                lines.add(Component.translatable(MagiEngineersStaff.KEY + ".tooltip.element",
                        element.getDisplayName().withColor(element.getTextColor().getRGB()),
                        Component.literal(percent(MagiEngineersStaffCompat.schoolPower(element, player))).withStyle(ChatFormatting.GRAY),
                        MagiEngineersStaffCompat.school(element, player).getDisplayName().copy()));
            }
            return;
        }
        lines.add(Component.translatable(MagiEngineersStaff.KEY + ".lore").withColor(0x808080));
        for (int i = 1; i <= 5; i++)
            lines.add(Component.translatable(MagiEngineersStaff.KEY + ".desc_" + i).withStyle(ChatFormatting.GRAY));
        lines.add(Component.empty());
        parameter(lines, "power", Component.literal(percent(player.getAttributeValue(AttributeRegistry.SPELL_POWER)))
                .withColor(ToolPropertyCategory.ATTACK.getColor().getRGB()));
        var highest = MagiEngineersStaffCompat.highestElement(player);
        for (Element element : new Element[]{highest, Element.MANA}) {
            parameter(lines, "elemental_power", element.getDisplayName().withColor(element.getTextColor().getRGB())
                    .append(" " + percent(MagiEngineersStaffCompat.schoolPower(element, player))));
        }
        parameter(lines, "charge_speed", coloured("+0%", ToolPropertyCategory.HANDLING));
        parameter(lines, "cooldown_speed", coloured("+0%", ToolPropertyCategory.CONTINUITY));
        parameter(lines, "launch", coloured("+50%", ToolPropertyCategory.RANGE));
        parameter(lines, "mana_efficiency", coloured("+0%", ToolPropertyCategory.UNIQUE));
        lines.add(Component.translatable(MagiEngineersStaff.KEY + ".detail_help").withStyle(ChatFormatting.GRAY));
    }

    private static Component coloured(String value, ToolPropertyCategory category) {
        return Component.literal(value).withColor(category.getColor().getRGB());
    }

    private static void parameter(List<Component> lines, String name, Component value) {
        lines.add(Component.translatable(MagiEngineersStaff.KEY + ".tooltip.param",
                Component.translatable("tool.magitech.property." + name).withColor(0xA0A0A0), value));
    }

    public static String percent(double value) {
        long percent = Math.round((value - 1) * 100);
        return (percent >= 0 ? "+" : "") + percent + "%";
    }
}
