package jp.aquafactory.apprenticecodex.spell.mirageavoidance;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.MirageAvoidanceState;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncMirageAvoidanceStatePacket;
import net.minecraft.server.level.ServerPlayer;

public final class MirageAvoidanceSync {
    private MirageAvoidanceSync() {
    }

    public static void syncToClient(ServerPlayer player, MirageAvoidanceState state) {
        Networks.sendToTrackingEntityAndSelf(player, new SyncMirageAvoidanceStatePacket(
                player.getId(),
                state.startGameTime,
                state.activeUntilGameTime,
                state.invulnerableUntilGameTime,
                state.movementForward,
                state.movementStrafe,
                state.suppressFallDamageUntilGround
        ));
    }
}
