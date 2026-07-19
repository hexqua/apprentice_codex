package jp.aquafactory.apprenticecodex.item.curios.attackcastring;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.AbstractRightClickMagicWeaponItem;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
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
        return tryTriggerAttack(player, hand, bypassChargeCheck, List.of());
    }

    public static boolean tryTriggerAttack(ServerPlayer player, InteractionHand hand, boolean bypassChargeCheck,
                                           List<BlockTargetData> ringTargets) {
        if (!bypassChargeCheck && !AbstractRightClickMagicWeaponItem.isFullyChargedAttack(player)) {
            return false;
        }

        var magicData = MagicData.getPlayerMagicData(player);
        if (magicData == null || magicData.isCasting()) {
            // 手持ち側の attemptInitiateCast も既存詠唱をキャンセルするため、指輪へのフォールバックより前に止める。
            return false;
        }

        var heldStack = player.getItemInHand(hand);
        if (heldStack.getItem() instanceof SwingTriggeredMagicItem swingTriggeredMagicItem
                && swingTriggeredMagicItem.canTriggerSpellOnSwing(player, hand)
                && swingTriggeredMagicItem.tryTriggerSpellOnSwing(player, hand, bypassChargeCheck)) {
            // 手持ち詠唱が開始できた攻撃では、同じ攻撃による指輪の多重詠唱を抑える。
            return true;
        }

        return tryTriggerEquippedRings(player, ringTargets);
    }

    public static boolean tryTriggerEquippedRings(ServerPlayer player) {
        return tryTriggerEquippedRings(player, List.of());
    }

    public static boolean tryTriggerEquippedRings(ServerPlayer player, List<BlockTargetData> ringTargets) {
        var casted = false;
        var equippedRings = getEquippedRings(player);
        for (var index = 0; index < equippedRings.size(); ++index) {
            var equippedRing = equippedRings.get(index);
            if (equippedRing.stack().getItem() instanceof AttackcastRing ring) {
                var spellData = getImbuedSpellData(equippedRing);
                if (spellData != SpellData.EMPTY) {
                    BlockTargetingHelper.setPendingServerTarget(
                            player,
                            spellData.getSpell().getSpellResource(),
                            index < ringTargets.size() ? ringTargets.get(index) : null
                    );
                } else {
                    BlockTargetingHelper.clearPendingServerTarget(player);
                }
                try {
                    if (ring.tryTriggerImbuedSpell(player, equippedRing.stack(), castingSlot(equippedRing))) {
                        casted = true;
                    }
                } finally {
                    // 失敗時に対象が消費されなくても、別の詠唱へ持ち越さない。
                    BlockTargetingHelper.clearPendingServerTarget(player);
                }
            }
        }
        return casted;
    }

    public static List<SpellData> getEquippedSpellData(Player player) {
        return getEquippedRings(player).stream()
                .map(AttackcastRingAttackTrigger::getImbuedSpellData)
                .toList();
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

    private static SpellData getImbuedSpellData(SlotResult result) {
        var container = ISpellContainer.get(result.stack());
        return container != null && container.getActiveSpellCount() > 0
                ? container.getSpellAtIndex(0)
                : SpellData.EMPTY;
    }
}
