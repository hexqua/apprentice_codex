package jp.aquafactory.apprenticecodex.item.curios.manasoultransducer;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ManaSoulTransducerClientEvents {
    private ManaSoulTransducerClientEvents() {}

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ManaSoulTransducerConfigState.reset();
    }
}
