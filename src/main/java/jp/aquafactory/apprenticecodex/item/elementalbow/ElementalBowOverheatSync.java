package jp.aquafactory.apprenticecodex.item.elementalbow;

import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncElementalBowOverheatPacket;
import net.minecraft.server.level.ServerPlayer;

public final class ElementalBowOverheatSync {
    private ElementalBowOverheatSync() {
    }

    public static void syncToClient(ServerPlayer player) {
        Networks.sendToPlayer(player, new SyncElementalBowOverheatPacket(
                ElementalBowOverheatManager.createSyncTag(player)
        ));
    }
}
