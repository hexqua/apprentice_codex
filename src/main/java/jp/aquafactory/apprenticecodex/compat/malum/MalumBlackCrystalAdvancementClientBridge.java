package jp.aquafactory.apprenticecodex.compat.malum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class MalumBlackCrystalAdvancementClientBridge {
    private MalumBlackCrystalAdvancementClientBridge() {
    }

    public static void register() {
        if (ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            MalumBlackCrystalAdvancementClientBridgeImpl.register();
        }
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (ModList.get().isLoaded(MalumCompatibility.MOD_ID)) {
            MalumBlackCrystalAdvancementClientBridgeImpl.sendIfRevealed();
        }
    }
}
