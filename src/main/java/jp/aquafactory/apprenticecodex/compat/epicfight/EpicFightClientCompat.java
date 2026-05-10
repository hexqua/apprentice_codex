package jp.aquafactory.apprenticecodex.compat.epicfight;

import com.mojang.blaze3d.platform.InputConstants;
import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import net.minecraft.client.KeyMapping;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.screen.config.ItemsPreferenceScreen;

public final class EpicFightClientCompat {
    public static final String MOD_ID = "epicfight";

    private EpicFightClientCompat() {
    }

    public static boolean isBattleMode() {
        var clientEngine = ClientEngine.getInstance();
        return clientEngine != null && clientEngine.isEpicFightMode();
    }

    public static void register() {
        ItemsPreferenceScreen.registerWeaponCategorizedItemClasses(MultipurposeStaffrifle.class);
    }

    public static boolean matchesAttackInput(InputConstants.Type type, int value) {
        return matchesKey(EpicFightInputAction.ATTACK.keyMapping(), type, value);
    }

    private static boolean matchesKey(KeyMapping keyMapping, InputConstants.Type type, int value) {
        var key = keyMapping.getKey();
        return key.getType() == type && key.getValue() == value;
    }
}
