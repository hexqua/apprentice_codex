package jp.aquafactory.apprenticecodex.item.curios.attackcastring;

import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Comparator;
import java.util.List;

public final class AttackcastRingAttackTrigger {
    private AttackcastRingAttackTrigger() {
    }

    public static boolean canTriggerAttack(Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof SwingTriggeredMagicItem swingTriggeredMagicItem
                && swingTriggeredMagicItem.canTriggerSpellOnSwing(player, hand)) {
            return true;
        }
        return hasEquippedRing(player);
    }

    public static boolean tryTriggerAttack(ServerPlayer player, InteractionHand hand, boolean bypassChargeCheck) {
        if (!bypassChargeCheck && !AbstractRightClickMagicWeaponItem.isFullyChargedAttack(player)) {
            return false;
        }

        var heldStack = player.getItemInHand(hand);
        if (heldStack.getItem() instanceof SwingTriggeredMagicItem swingTriggeredMagicItem
                && swingTriggeredMagicItem.canTriggerSpellOnSwing(player, hand)
                && swingTriggeredMagicItem.tryTriggerSpellOnSwing(player, hand, bypassChargeCheck)) {
            // 手持ち詠唱が開始できた攻撃では、同じ攻撃による指輪の多重詠唱を抑える。
            return true;
        }

        return tryTriggerEquippedRings(player);
    }

    public static boolean tryTriggerEquippedRings(ServerPlayer player) {
        var casted = false;
        for (var equippedRing : getEquippedRings(player)) {
            if (equippedRing.stack().getItem() instanceof AttackcastRing ring
                    && ring.tryTriggerImbuedSpell(player, equippedRing.stack(), castingSlot(equippedRing))) {
                casted = true;
            }
        }
        return casted;
    }

    public static boolean hasEquippedRing(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findFirstCurio(stack -> stack.getItem() instanceof AttackcastRing).isPresent())
                .orElse(false);
    }

    static List<SlotResult> getEquippedRings(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .map(inventory -> inventory.findCurios(stack -> stack.getItem() instanceof AttackcastRing).stream()
                        .sorted(Comparator
                                .comparing((SlotResult result) -> result.slotContext().identifier())
                                .thenComparingInt(result -> result.slotContext().index()))
                        .toList())
                .orElse(List.of());
    }

    private static String castingSlot(SlotResult result) {
        return result.slotContext().identifier() + "_" + result.slotContext().index();
    }
}
