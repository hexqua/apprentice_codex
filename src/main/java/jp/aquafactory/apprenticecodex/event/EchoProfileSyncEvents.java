package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncEchoProfileSpellIdsPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class EchoProfileSyncEvents {
    private EchoProfileSyncEvents() {
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        var packet = new SyncEchoProfileSpellIdsPacket(
                MulticastEchoStaffAttackProfileManager.createProfileSpellIdSnapshot()
        );
        for (var player : event.getPlayers()) {
            Networks.sendToPlayer(player, packet);
        }
    }

    @Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            MulticastEchoStaffAttackProfileManager.clearClientSyncedProfileSpellIds();
        }
    }
}
