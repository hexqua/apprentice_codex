package jp.aquafactory.apprenticecodex.item;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.OffhandUpgradeAttributeEvent;
import jp.aquafactory.apprenticecodex.item.spellchargedgreatsword.SpellchargedGreatsword;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class MagicItemAttributeEvent {
    private MagicItemAttributeEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        var stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        var item = stack.getItem();
        if (item instanceof AbstractSpellGunItem spellGunItem) {
            // Iron's 側も同じイベントで UpgradeData を差し込むため、
            // stack 固有補正の差し戻しは最後に行って二重適用を防ぐ。
            // 1.21.1 では ATTRIBUTE_MODIFIERS component が stack-sensitive override を上書きし得るため、
            // item 側で定義した正規の計算結果をイベント経由で差し戻す。
            replaceModifiers(event, spellGunItem.getDefaultAttributeModifiers(stack));
            applyMainhandUpgradeBridge(event);
            return;
        }

        if (item instanceof AbstractOffhandMagicItem offhandMagicItem) {
            replaceModifiers(event, offhandMagicItem.getDefaultAttributeModifiers(stack));
            OffhandUpgradeAttributeEvent.onItemAttributeModifier(event);
            return;
        }

        if (item instanceof AbstractRightClickMagicWeaponItem rightClickMagicWeaponItem) {
            replaceModifiers(event, rightClickMagicWeaponItem.getDefaultAttributeModifiers(stack));
            applyMainhandUpgradeBridge(event);
            return;
        }

        if (item instanceof SpellchargedGreatsword spellchargedGreatsword) {
            replaceModifiers(event, spellchargedGreatsword.getDefaultAttributeModifiers(stack));
        }
    }

    private static void applyMainhandUpgradeBridge(ItemAttributeModifierEvent event) {
        OffhandUpgradeAttributeEvent.applyStoredUpgradeData(
                event,
                EquipmentSlot.MAINHAND.getName(),
                EquipmentSlot.MAINHAND.getName(),
                EquipmentSlotGroup.MAINHAND
        );
    }

    private static void replaceModifiers(ItemAttributeModifierEvent event, net.minecraft.world.item.component.ItemAttributeModifiers modifiers) {
        event.clearModifiers();
        for (var entry : modifiers.modifiers()) {
            event.addModifier(entry.attribute(), entry.modifier(), entry.slot());
        }
    }
}
