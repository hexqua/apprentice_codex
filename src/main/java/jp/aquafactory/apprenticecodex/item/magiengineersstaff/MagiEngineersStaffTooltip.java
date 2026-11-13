package jp.aquafactory.apprenticecodex.item.magiengineersstaff;

import jp.aquafactory.apprenticecodex.compat.magitech.MagitechStaffTooltip;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class MagiEngineersStaffTooltip {
    private MagiEngineersStaffTooltip() {
    }

    public static void append(List<Component> lines) {
        if (MagiEngineersStaff.available()) MagitechStaffTooltip.append(lines);
        else lines.add(Component.translatable(MagiEngineersStaff.KEY + ".lore").withColor(0x808080));
    }
}
