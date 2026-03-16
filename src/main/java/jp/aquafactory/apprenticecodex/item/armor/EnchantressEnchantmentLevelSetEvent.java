package jp.aquafactory.apprenticecodex.item.armor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.EnchantmentLevelSetEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class EnchantressEnchantmentLevelSetEvent {
    private EnchantressEnchantmentLevelSetEvent() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEnchantmentLevelSet(EnchantmentLevelSetEvent event) {
        if (EnchantressEnchantingTableBonusHelper.isFeatureDisabled()
                || event.getEnchantRow() != EnchantressEnchantingTableBonusHelper.TARGET_ENCHANT_ROW
                || event.getEnchantLevel() <= 0) {
            return;
        }

        // Forge イベントは使用プレイヤーを直接持たないため、現在テーブルを開いている近傍プレイヤーから推定する。
        var bonus = EnchantressEnchantingTableBonusHelper.getBonusForNearbyEnchantingPlayer(event.getLevel(), event.getPos());
        if (bonus <= 0) {
            return;
        }

        event.setEnchantLevel(event.getEnchantLevel() + bonus);
    }
}
