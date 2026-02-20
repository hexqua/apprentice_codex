package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.item.curios.endergrimoire.EnderGrimoire;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class EnderGrimoireSpellSelectionEvents {
    private EnderGrimoireSpellSelectionEvents() {
    }

    @SubscribeEvent
    public static void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        var player = event.getEntity();
        var spellbookStack = Utils.getPlayerSpellbookStack(player);
        if (spellbookStack == null || !(spellbookStack.getItem() instanceof EnderGrimoire)) {
            return;
        }

        player.getCapability(Capabilities.ENDER_GRIMOIRE_SPELLBOOK).ifPresent(data -> {
            var activeSpells = data.getSpellContainer().getActiveSpells();
            for (int i = 0; i < activeSpells.size(); ++i) {
                event.addSelectionOption(activeSpells.get(i).spellData(), Curios.SPELLBOOK_SLOT, i);
            }
        });
    }
}
