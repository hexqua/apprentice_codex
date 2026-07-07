package jp.aquafactory.apprenticecodex.spell.remoteeye;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.RemoteEyeState;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncRemoteEyeStatePacket;
import net.minecraft.server.level.ServerPlayer;

public final class RemoteEyeSync {
    private RemoteEyeSync() {
    }

    public static void syncToClient(ServerPlayer player, RemoteEyeState state) {
        Networks.sendToPlayer(player, new SyncRemoteEyeStatePacket(
                state.activeUntilGameTime,
                state.activeDurationTicks,
                state.anchorX,
                state.anchorY,
                state.anchorZ,
                state.anchorYaw,
                state.anchorPitch
        ));
    }
}
