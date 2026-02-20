package jp.aquafactory.apprenticecodex.capability.endergrimoire;

import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
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

    public static void editAndSync(ServerPlayer serverPlayer, Consumer<ISpellContainerMutable> editor) {
        serverPlayer.getCapability(Capabilities.ENDER_GRIMOIRE_SPELLBOOK).ifPresent(data -> {
            data.edit(editor);
            syncToClient(serverPlayer);
        });
    }
}
