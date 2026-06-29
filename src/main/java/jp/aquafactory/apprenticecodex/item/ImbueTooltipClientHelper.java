package jp.aquafactory.apprenticecodex.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ImbueTooltipClientHelper {
    private ImbueTooltipClientHelper() {
    }

    static boolean hasDetailsKeyDown() {
        return Screen.hasShiftDown() || Screen.hasAltDown();
    }

    static Component getAttackKeyName() {
        return Minecraft.getInstance().options.keyAttack.getTranslatedKeyMessage();
    }

    static Component getUseKeyName() {
        return Minecraft.getInstance().options.keyUse.getTranslatedKeyMessage();
    }

    static Component getJumpKeyName() {
        return Minecraft.getInstance().options.keyJump.getTranslatedKeyMessage();
    }
}
