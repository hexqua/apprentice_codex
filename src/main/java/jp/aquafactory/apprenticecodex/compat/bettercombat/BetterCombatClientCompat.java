package jp.aquafactory.apprenticecodex.compat.bettercombat;

import jp.aquafactory.apprenticecodex.event.client.ClientSwingMagicAttackTrigger;
import jp.aquafactory.apprenticecodex.event.client.ClientMultipurposeStaffrifleInputEvent;
import jp.aquafactory.apprenticecodex.event.client.MultipurposeStaffrifleClientAdsState;
import jp.aquafactory.apprenticecodex.item.MultipurposeStaffrifle;
import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.api.client.BetterCombatClientEvents;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

public final class BetterCombatClientCompat {
    public static final String MOD_ID = "bettercombat";

    private static boolean registered;

    private BetterCombatClientCompat() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        // Better Combat 側で実際に攻撃判定を出す瞬間へ合わせ、演出や当たり判定とのズレを避ける。
        BetterCombatClientEvents.ATTACK_HIT.register(BetterCombatClientCompat::onAttackHit);
        registered = true;
    }

    public static boolean usesBetterCombatAttackTiming(LocalPlayer player) {
        if (player == null) {
            return false;
        }

        var attributes = WeaponRegistry.getAttributes(player.getMainHandItem());
        return hasAttackSequence(attributes);
    }

    private static void onAttackHit(LocalPlayer player, AttackHand attackHand, List<Entity> targets, Entity cursorTarget) {
        if (attackHand.isOffHand()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (player != minecraft.player) {
            return;
        }

        if (player.getMainHandItem().getItem() instanceof MultipurposeStaffrifle
                && !MultipurposeStaffrifleClientAdsState.isLocalAdsKeyHeld(player)) {
            ClientMultipurposeStaffrifleInputEvent.trySendNonAdsSpecialCast(minecraft);
        }
        ClientSwingMagicAttackTrigger.trySendForBetterCombat(minecraft);
    }

    private static boolean hasAttackSequence(WeaponAttributes attributes) {
        return attributes != null && attributes.attacks() != null;
    }
}
