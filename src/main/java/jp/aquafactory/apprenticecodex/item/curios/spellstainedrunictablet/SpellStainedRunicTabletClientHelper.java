package jp.aquafactory.apprenticecodex.item.curios.spellstainedrunictablet;

import net.minecraft.client.gui.screens.Screen;

final class SpellStainedRunicTabletClientHelper {
    private SpellStainedRunicTabletClientHelper() {
    }

    static boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }
}
