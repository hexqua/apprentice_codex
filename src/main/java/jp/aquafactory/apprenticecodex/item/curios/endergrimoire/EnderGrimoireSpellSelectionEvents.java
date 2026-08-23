package jp.aquafactory.apprenticecodex.item.curios.endergrimoire;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.compat.Curios;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class EnderGrimoireSpellSelectionEvents {
    private EnderGrimoireSpellSelectionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        var player = event.getEntity();
        var spellbookStack = Utils.getPlayerSpellbookStack(player);
        if (spellbookStack == null || !(spellbookStack.getItem() instanceof EnderGrimoire)) {
            return;
        }

        Capabilities.getEnderGrimoireSpellbook(player).ifPresent(data -> {
            var activeSpells = data.getSpellContainer().getActiveSpells();
            for (int i = 0; i < activeSpells.size(); ++i) {
                event.addSelectionOption(activeSpells.get(i).spellData(), Curios.SPELLBOOK_SLOT, i);
            }
        });
    }
}

