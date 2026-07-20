package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.util.UpgradeUtils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.OffhandAttributeRelocatingItem;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.List;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class OffhandUpgradeAttributeEvent {
    private OffhandUpgradeAttributeEvent() {
    }

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        var stack = event.getItemStack();
        var relocatesAttributes = stack.getItem() instanceof OffhandAttributeRelocatingItem item
                && item.usesOffhandAttributeModifiers(stack);
        if (!(stack.getItem() instanceof AbstractOffhandMagicItem) && !relocatesAttributes) {
            return;
        }

        if (relocatesAttributes) {
            removeMainhandUpgradeModifiers(event);
        }

        applyStoredUpgradeData(
                event,
                EquipmentSlot.MAINHAND.getName(),
            EquipmentSlot.OFFHAND.getName(),
            EquipmentSlotGroup.OFFHAND
        );
    }

    public static void applyStoredUpgradeData(
            ItemAttributeModifierEvent event,
            String expectedStoredSlot,
            String appliedSlotId,
            EquipmentSlotGroup targetSlotGroup
    ) {
        var stack = event.getItemStack();

        var upgradeData = UpgradeData.getUpgradeData(stack);
        if (upgradeData == UpgradeData.NONE) {
            return;
        }
        if (!expectedStoredSlot.equals(upgradeData.getUpgradedSlot())) {
            return;
        }

        // Iron's 1.20.1 は非防具・非 Curio を mainhand 扱いで保存するため、
        // 1.21.1 側へ移植する際は本橋渡しがまだ必要か依存側実装を再確認する。
        UpgradeUtils.handleAttributeEvent(
                event.getModifiers(),
                upgradeData,
                (attribute, modifier) -> event.addModifier(attribute, modifier, targetSlotGroup),
                (attribute, modifier) -> event.removeModifier(attribute, modifier.id()),
                appliedSlotId
        );
    }

    private static void removeMainhandUpgradeModifiers(ItemAttributeModifierEvent event) {
        var mainhandPrefix = "mainhand_upgrade_";
        for (var entry : List.copyOf(event.getModifiers())) {
            var modifierId = entry.modifier().id();
            if ("irons_spellbooks".equals(modifierId.getNamespace())
                    && modifierId.getPath().startsWith(mainhandPrefix)) {
                event.removeModifier(entry.attribute(), modifierId);
            }
        }
    }
}
