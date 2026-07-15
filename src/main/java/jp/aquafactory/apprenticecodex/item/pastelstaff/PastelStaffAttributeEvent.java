package jp.aquafactory.apprenticecodex.item.pastelstaff;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PastelStaffAttributeEvent {
    private PastelStaffAttributeEvent() {
    }

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        var stack = event.getItemStack();
        if (!PastelStaff.isPastelStaff(stack)) {
            return;
        }

        // 1.21.1 では ATTRIBUTE_MODIFIERS component があると stack-sensitive override が実運用で無視される。
        // 実際の装備反映経路に合わせてここで PastelStaff 固有の補正を差し込む。
        var modifiers = PastelStaff.buildAttributeModifiers(stack, event.getDefaultModifiers());
        event.clearModifiers();
        for (var entry : modifiers.modifiers()) {
            event.addModifier(entry.attribute(), entry.modifier(), entry.slot());
        }
    }
}
