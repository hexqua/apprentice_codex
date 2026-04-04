package jp.aquafactory.apprenticecodex.event;

import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.util.UpgradeUtils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.AbstractOffhandMagicItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class OffhandUpgradeAttributeEvent {
    private OffhandUpgradeAttributeEvent() {
    }

    @SubscribeEvent
    public static void onItemAttributeModifier(ItemAttributeModifierEvent event) {
        var stack = event.getItemStack();
        if (!(stack.getItem() instanceof AbstractOffhandMagicItem)) {
            return;
        }
        if (event.getSlotType() != EquipmentSlot.OFFHAND) {
            return;
        }

        var upgradeData = UpgradeData.getUpgradeData(stack);
        if (upgradeData == UpgradeData.NONE) {
            return;
        }
        if (!EquipmentSlot.MAINHAND.getName().equals(upgradeData.getUpgradedSlot())) {
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
}
