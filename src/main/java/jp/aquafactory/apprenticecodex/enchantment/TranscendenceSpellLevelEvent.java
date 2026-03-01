package jp.aquafactory.apprenticecodex.enchantment;

import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class TranscendenceSpellLevelEvent {
    private TranscendenceSpellLevelEvent() {
    }

    @SubscribeEvent
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        var caster = event.getEntity();
        if (caster == null) {
            return;
        }

        var offhandStack = caster.getOffhandItem();
        if (!(offhandStack.getItem() instanceof AbstractOffhandMagicItem)) {
            return;
        }

        var transcendenceLevel = Enchantments.getLevel(offhandStack, Enchantments.TRANSCENDENCE);
        if (transcendenceLevel <= 0 || !ISpellContainer.isSpellContainer(offhandStack)) {
            return;
        }

        var spellContainer = ISpellContainer.get(offhandStack);
        if (spellContainer == null || spellContainer.getActiveSpellCount() <= 0) {
            return;
        }

        var imbuedSpell = spellContainer.getSpellAtIndex(0);
        if (imbuedSpell == SpellData.EMPTY || !imbuedSpell.getSpell().equals(event.getSpell())) {
            return;
        }

        event.addLevels(transcendenceLevel);
    }
}
