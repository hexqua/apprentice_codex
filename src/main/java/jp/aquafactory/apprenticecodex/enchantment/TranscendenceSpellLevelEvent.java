package jp.aquafactory.apprenticecodex.enchantment;

import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.CuriosSlotConstants;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class TranscendenceSpellLevelEvent {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private TranscendenceSpellLevelEvent() {
    }

    @SubscribeEvent
    public static void onModifySpellLevel(ModifySpellLevelEvent event) {
        var caster = event.getEntity();
        if (caster == null) {
            return;
        }

        var candidates = new ArrayList<TranscendenceResolver.Candidate>();
        candidates.add(new TranscendenceResolver.Candidate(caster.getMainHandItem(), true));
        candidates.add(new TranscendenceResolver.Candidate(caster.getOffhandItem(), true));
        for (var slot : ARMOR_SLOTS) {
            candidates.add(new TranscendenceResolver.Candidate(caster.getItemBySlot(slot), false));
        }

        // Curios の識別子と inventory API は loader 接着側だけで扱い、共通 Resolver には持ち込まない。
        CuriosApi.getCuriosInventory(caster).ifPresent(inventory ->
                inventory.findCurios(stack -> stack.getItem() instanceof TranscendencePolicy).stream()
                        .map(result -> new TranscendenceResolver.Candidate(result.stack(), false))
                        .forEach(candidates::add)
        );

        var addedLevel = TranscendenceResolver.resolveMaxEventLevel(
                event.getSpell(),
                candidates,
                stack -> Enchantments.getLevel(stack, Enchantments.TRANSCENDENCE)
        );
        if (addedLevel > 0) {
            event.addLevels(addedLevel);
        }
    }
}
