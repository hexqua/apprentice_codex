package jp.aquafactory.apprenticecodex.compat.bettercombat;

import jp.aquafactory.apprenticecodex.item.BetterCombatOffhandDualWieldingPolicyItem;
import net.minecraft.world.entity.player.Player;

public final class BetterCombatDualWieldingPolicyCompat {
    private BetterCombatDualWieldingPolicyCompat() {
    }

    public static boolean shouldSuppressDualWielding(Player player) {
        if (player == null) {
            return false;
        }

        var mainHandStack = player.getMainHandItem();
        var offhandStack = player.getOffhandItem();
        if (mainHandStack.isEmpty() || offhandStack.isEmpty()) {
            return false;
        }

        return offhandStack.getItem() instanceof BetterCombatOffhandDualWieldingPolicyItem policyItem
                && policyItem.suppressesBetterCombatOffhandDualWielding(offhandStack, mainHandStack);
    }
}
