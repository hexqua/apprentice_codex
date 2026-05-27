package jp.aquafactory.apprenticecodex.compat.bettercombat;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.minecraft.world.entity.player.Player;

public final class BetterCombatOffhandSpellSelectionRescueCompat {
    private BetterCombatOffhandSpellSelectionRescueCompat() {
    }

    public static void appendSelectionIfNeeded(SpellSelectionManager.SpellSelectionEvent event) {
        if (!event.getManager().getSpellsForSlot(SpellSelectionManager.OFFHAND).isEmpty()) {
            return;
        }

        appendOffhandMagicItemSelectionIfNeeded(event);
        appendScrollcasterGauntletSelectionIfNeeded(event);
    }

    private static void appendOffhandMagicItemSelectionIfNeeded(SpellSelectionManager.SpellSelectionEvent event) {
        var player = event.getEntity();
        if (!BetterCombatOffhandAttributeRescueCompat.isRescueActive(player)) {
            return;
        }

        var offhandStack = BetterCombatOffhandAttributeRescueCompat.getPhysicalOffhandStack(player);
        if (!(offhandStack.getItem() instanceof AbstractOffhandMagicItem offhandMagicItem)) {
            return;
        }

        if (!ISpellContainer.isSpellContainer(offhandStack)) {
            // Better Combat は EquipmentSlot.OFFHAND 参照を空にするため、
            // SpellSelectionManager 初期化前にコンテナ生成が走っていない stack だけ補完する。
            offhandMagicItem.initializeSpellContainer(offhandStack);
        }

        var spellData = resolveFixedOffhandSpell(player);
        if (spellData == SpellData.EMPTY) {
            return;
        }

        // AbstractOffhandMagicItem は 1 スロット前提なので、固定枠 0 だけを wheel に戻す。
        event.addSelectionOption(spellData, SpellSelectionManager.OFFHAND, 0);
    }

    private static void appendScrollcasterGauntletSelectionIfNeeded(SpellSelectionManager.SpellSelectionEvent event) {
        if (!event.getManager().getSpellsForSlot(SpellSelectionManager.OFFHAND).isEmpty()) {
            return;
        }

        var player = event.getEntity();
        if (!BetterCombatScrollcasterGauntletCompat.isRescueActive(player)) {
            return;
        }

        var spellData = BetterCombatScrollcasterGauntletCompat.getSelectedOffhandSpell(player);
        if (spellData == SpellData.EMPTY) {
            return;
        }

        // Scrollcaster Gauntlet は選択中スクロールだけを魔法ホルダーとして wheel に戻す。
        event.addSelectionOption(spellData, SpellSelectionManager.OFFHAND, 0);
    }

    private static SpellData resolveFixedOffhandSpell(Player player) {
        var offhandStack = BetterCombatOffhandAttributeRescueCompat.getPhysicalOffhandStack(player);
        if (!ISpellContainer.isSpellContainer(offhandStack)) {
            return SpellData.EMPTY;
        }

        var spellContainer = ISpellContainer.get(offhandStack);
        if (spellContainer == null || !spellContainer.isSpellWheel() || spellContainer.getActiveSpellCount() <= 0) {
            return SpellData.EMPTY;
        }

        return spellContainer.getSpellAtIndex(0);
    }
}
