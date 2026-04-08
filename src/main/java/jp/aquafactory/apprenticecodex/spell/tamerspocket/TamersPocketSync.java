package jp.aquafactory.apprenticecodex.spell.tamerspocket;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncTamersPocketCountPacket;
import net.minecraft.server.level.ServerPlayer;

public final class TamersPocketSync {
    private TamersPocketSync() {
    }

    public static void syncToClient(ServerPlayer player) {
        var spellData = Capabilities.getSpellDataOrNull(player);
        if (spellData == null) {
            return;
        }

        var count = spellData.get(CodexSpellStateTypeRegister.TAMERS_POCKET_STATE).getStoredPetCount();
        Networks.sendToPlayer(player, new SyncTamersPocketCountPacket(count));
    }
}
