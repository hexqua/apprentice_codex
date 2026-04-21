package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientCastState;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientLoanState;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientPresentationState;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientFocusStaffbowCancelPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
public final class FocusStaffbowClientEvents {
    private FocusStaffbowClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        var player = Minecraft.getInstance().player;
        if (player == null || event.getNewScreen() == null) {
            return;
        }
        if (!FocusStaffbowClientCastState.hasPendingCast(player)) {
            return;
        }

        Networks.sendToServer(new ClientFocusStaffbowCancelPacket());
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FocusStaffbowClientCastState.clear();
        FocusStaffbowClientLoanState.clear();
        FocusStaffbowClientPresentationState.clearAll();
    }
}
