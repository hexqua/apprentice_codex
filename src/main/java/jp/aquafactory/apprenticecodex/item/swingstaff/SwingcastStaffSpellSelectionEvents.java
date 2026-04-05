package jp.aquafactory.apprenticecodex.item.swingstaff;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SwingcastStaffSpellSelectionEvents {
    private SwingcastStaffSpellSelectionEvents() {
    }

    @SubscribeEvent
    public static void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        var player = event.getEntity();
        var mainHandStack = player.getMainHandItem();
        if (!(mainHandStack.getItem() instanceof AbstractSwingcastStaffItem swingcastStaffItem)) {
            return;
        }

        if (!swingcastStaffItem.allowImbuedSpellInSpellWheel(mainHandStack)) {
            return;
        }

        var spellData = swingcastStaffItem.getImbuedSpellData(mainHandStack);
        if (spellData == null || !swingcastStaffItem.canImbueSpell(spellData)) {
            return;
        }

        event.addSelectionOption(spellData, SpellSelectionManager.MAINHAND, 0);
    }
}
