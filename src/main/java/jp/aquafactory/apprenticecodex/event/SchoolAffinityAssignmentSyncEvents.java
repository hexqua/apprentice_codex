package jp.aquafactory.apprenticecodex.event;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncSchoolAffinityAssignmentsPacket;
import jp.aquafactory.apprenticecodex.utility.SchoolAffinityRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class SchoolAffinityAssignmentSyncEvents {
    private SchoolAffinityAssignmentSyncEvents() {
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        var packet = new SyncSchoolAffinityAssignmentsPacket(
                SchoolAffinityRegistry.createAssignmentSnapshot(),
                SchoolAffinityRegistry.createCatalystBindingSnapshot()
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
            SchoolAffinityRegistry.clearSyncedAssignments();
        }
    }
}
