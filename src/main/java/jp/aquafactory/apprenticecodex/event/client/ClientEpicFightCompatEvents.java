package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightClientCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class ClientEpicFightCompatEvents {
    private ClientEpicFightCompatEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ModList.get().isLoaded(EpicFightClientCompat.MOD_ID)) {
            return;
        }

        EpicFightClientCompat.tick();
    }

    @SubscribeEvent
    public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (ModList.get().isLoaded(EpicFightClientCompat.MOD_ID)) {
            EpicFightClientCompat.clear();
        }
    }
}
