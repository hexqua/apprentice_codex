package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileManager;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncAutocastAmuletProfileSpellIdsPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class AutocastAmuletSpellProfileSyncEvents {
    private AutocastAmuletSpellProfileSyncEvents() {
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        var packet = new SyncAutocastAmuletProfileSpellIdsPacket(
                AutocastAmuletSpellProfileManager.createProfileSpellIdSnapshot()
        );
        for (var player : event.getRelevantPlayers().toList()) {
            Networks.sendToPlayer(player, packet);
        }
    }

    @EventBusSubscriber(modid = ApprenticeCodex.MODID, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            AutocastAmuletSpellProfileManager.clearClientSyncedProfileSpellIds();
        }
    }
}
