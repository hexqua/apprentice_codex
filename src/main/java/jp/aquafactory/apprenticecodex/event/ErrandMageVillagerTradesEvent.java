package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.errandmage.ErrandMageTradeManager;
import jp.aquafactory.apprenticecodex.registry.VillagerProfessionRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class ErrandMageVillagerTradesEvent {
    private ErrandMageVillagerTradesEvent() {
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfessionRegistry.ERRAND_MAGE.get()) {
            return;
        }

        ErrandMageTradeManager.addTrades(event.getTrades());
    }
}
