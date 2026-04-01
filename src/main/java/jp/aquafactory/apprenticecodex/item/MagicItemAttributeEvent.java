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
            // 1.21.1 では ATTRIBUTE_MODIFIERS component が stack-sensitive override を上書きし得るため、
            // item 側で定義した正規の計算結果をイベント経由で差し戻す。
            replaceModifiers(event, spellGunItem.getDefaultAttributeModifiers(stack));
            return;
        }

        if (item instanceof AbstractOffhandMagicItem offhandMagicItem) {
            replaceModifiers(event, offhandMagicItem.getDefaultAttributeModifiers(stack));
            return;
        }

        if (item instanceof AbstractRightClickMagicWeaponItem rightClickMagicWeaponItem) {
            replaceModifiers(event, rightClickMagicWeaponItem.getDefaultAttributeModifiers(stack));
        }
    }

    private static void replaceModifiers(ItemAttributeModifierEvent event, net.minecraft.world.item.component.ItemAttributeModifiers modifiers) {
        event.clearModifiers();
        for (var entry : modifiers.modifiers()) {
            event.addModifier(entry.attribute(), entry.modifier(), entry.slot());
        }
    }
}
