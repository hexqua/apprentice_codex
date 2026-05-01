package jp.aquafactory.apprenticecodex.item.circuitheatstaff;

import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncCircuitHeatStaffOverheatPacket;
import net.minecraft.server.level.ServerPlayer;

public final class CircuitHeatStaffOverheatSync {
    private CircuitHeatStaffOverheatSync() {
    }

    public static void syncToClient(ServerPlayer player) {
        Networks.sendToPlayer(player, new SyncCircuitHeatStaffOverheatPacket(
                CircuitHeatStaffOverheatManager.createSyncTag(player)
        ));
    }
}
