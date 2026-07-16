package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.util.UpgradeUtils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.offhand.AbstractOffhandMagicItem;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class OffhandUpgradeAttributeEvent {
    private OffhandUpgradeAttributeEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        var stack = event.getItemStack();
        var adjustedSpellgun = AbstractSpellGunItem.usesOffhandAttributeModifiers(stack);
        if (!(stack.getItem() instanceof AbstractOffhandMagicItem) && !adjustedSpellgun) {
            return;
        }

        var upgradeData = UpgradeData.getUpgradeData(stack);
        if (upgradeData == UpgradeData.NONE) {
            return;
        }
        if (!EquipmentSlot.MAINHAND.getName().equals(upgradeData.getUpgradedSlot())) {
            return;
        }

        if (adjustedSpellgun && event.getSlotType() == EquipmentSlot.MAINHAND) {
            removeMainhandUpgradeModifiers(event, upgradeData);
            return;
        }
        if (event.getSlotType() != EquipmentSlot.OFFHAND) {
            return;
        }

        // Iron's 1.20.1 は非防具・非 Curio を mainhand 扱いで保存するため、
        // 1.21.1 側へ移植する際は本橋渡しがまだ必要か依存側実装を再確認する。
        UpgradeUtils.handleAttributeEvent(
                event.getModifiers(),
                upgradeData,
                event::addModifier,
                event::removeModifier,
                Optional.of(UpgradeUtils.UUIDForSlot(EquipmentSlot.OFFHAND))
        );
    }

    private static void removeMainhandUpgradeModifiers(ItemAttributeModifierEvent event, UpgradeData upgradeData) {
        var mainhandUpgradeId = UpgradeUtils.UUIDForSlot(EquipmentSlot.MAINHAND);
        for (var upgradeTypeHolder : upgradeData.upgrades().keySet()) {
            var attribute = upgradeTypeHolder.get().attribute().get();
            for (var modifier : List.copyOf(event.getModifiers().get(attribute))) {
                if (mainhandUpgradeId.equals(modifier.getId())) {
                    event.removeModifier(attribute, modifier);
                }
            }
        }
    }
}
