package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MagicItemAttributeEvent {
    private MagicItemAttributeEvent() {
    }

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        var stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        var item = stack.getItem();
        if (item instanceof AbstractSpellGunItem spellGunItem) {
            // 1.21.1 では ATTRIBUTE_MODIFIERS component 経路が優先されるため、
            // 実運用で参照される default modifiers を基準に enchant 分を差し戻す。
            replaceModifiers(event, spellGunItem.buildRuntimeAttributeModifiers(stack, event.getDefaultModifiers()));
            return;
        }

        if (item instanceof AbstractOffhandMagicItem offhandMagicItem) {
            replaceModifiers(event, offhandMagicItem.buildRuntimeAttributeModifiers(stack, event.getDefaultModifiers()));
        }
    }

    private static void replaceModifiers(ItemAttributeModifierEvent event, net.minecraft.world.item.component.ItemAttributeModifiers modifiers) {
        event.clearModifiers();
        for (var entry : modifiers.modifiers()) {
            event.addModifier(entry.attribute(), entry.modifier(), entry.slot());
        }
    }
}
