package jp.aquafactory.apprenticecodex.capability.endergrimoire;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncEnderGrimoireSpellbookPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public final class EnderGrimoireSpellbookSync {
    private EnderGrimoireSpellbookSync() {
    }

    public static void syncToClient(ServerPlayer serverPlayer) {
        serverPlayer.getCapability(Capabilities.ENDER_GRIMOIRE_SPELLBOOK).ifPresent(data ->
                Networks.sendToPlayer(serverPlayer, new SyncEnderGrimoireSpellbookPacket(data.save()))
        );
    }
}
