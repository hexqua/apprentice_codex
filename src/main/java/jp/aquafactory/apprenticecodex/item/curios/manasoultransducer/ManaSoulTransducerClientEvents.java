package jp.aquafactory.apprenticecodex.item.curios.manasoultransducer;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ManaSoulTransducerClientEvents {
    private ManaSoulTransducerClientEvents() {}

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ManaSoulTransducerConfigState.reset();
    }
}
