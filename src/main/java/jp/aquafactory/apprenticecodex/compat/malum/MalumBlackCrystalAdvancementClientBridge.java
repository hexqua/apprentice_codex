package jp.aquafactory.apprenticecodex.compat.malum;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class MalumBlackCrystalAdvancementClientBridge {
    private MalumBlackCrystalAdvancementClientBridge() {
    }

    public static void register() {
        if (ModList.get().isLoaded("malum")) {
            MalumBlackCrystalAdvancementClientBridgeImpl.register();
        }
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (ModList.get().isLoaded("malum")) {
            MalumBlackCrystalAdvancementClientBridgeImpl.sendIfRevealed();
        }
    }
}
