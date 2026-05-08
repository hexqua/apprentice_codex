package jp.aquafactory.apprenticecodex.item;

import net.minecraft.client.gui.screens.Screen;

final class ImbueTooltipClientHelper {
    private ImbueTooltipClientHelper() {
    }

    static boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }
}
