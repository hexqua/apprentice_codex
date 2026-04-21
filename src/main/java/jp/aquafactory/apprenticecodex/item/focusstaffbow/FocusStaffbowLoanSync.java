package jp.aquafactory.apprenticecodex.item.focusstaffbow;

import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncFocusStaffbowLoanPacket;
import net.minecraft.server.level.ServerPlayer;

public final class FocusStaffbowLoanSync {
    private FocusStaffbowLoanSync() {
    }

    public static void syncToClient(ServerPlayer player) {
        Networks.sendToPlayer(player, new SyncFocusStaffbowLoanPacket(
                FocusStaffbowCastManager.getOutstandingLoanMana(player)
        ));
    }
}
