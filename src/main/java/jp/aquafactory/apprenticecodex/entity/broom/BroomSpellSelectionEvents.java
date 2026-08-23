package jp.aquafactory.apprenticecodex.entity.broom;

import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.broom.AbstractBroomItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class BroomSpellSelectionEvents {
    public static final String SPELL_SELECTION_SLOT = "apprenticecodex_broom_scrolls";

    private BroomSpellSelectionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onSpellSelection(SpellSelectionManager.SpellSelectionEvent event) {
        var player = event.getEntity();
        if (!(player.getVehicle() instanceof AbstractBroomEntity broom)
                || broom.getControllingPassenger() != player) {
            return;
        }

        var broomStack = broom.getBroomItemStack();
        var enabledSlots = AbstractBroomItem.getEnabledCalibrationScrollSlotCount(broomStack);
        var selectionIndex = 0;
        for (var slot = 0; slot < enabledSlots; ++slot) {
            var spellData = AbstractBroomItem.getCalibrationSpellData(broomStack, slot);
            if (spellData == SpellData.EMPTY || spellData.getSpell() == null) {
                continue;
            }
            // Iron'sのselection indexは空の保存枠を含めず、公開中の選択肢だけで連番にする。
            event.addSelectionOption(spellData, SPELL_SELECTION_SLOT, selectionIndex++);
        }
    }
}
