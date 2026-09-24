package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.utility.AdvancementTools;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientMalumBlackCrystalRevealedPacket() {
    public static void encode(ClientMalumBlackCrystalRevealedPacket packet, FriendlyByteBuf buffer) {
    }

    public static ClientMalumBlackCrystalRevealedPacket decode(FriendlyByteBuf buffer) {
        return new ClientMalumBlackCrystalRevealedPacket();
    }

    public static void handle(ClientMalumBlackCrystalRevealedPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                AdvancementTools.award(player, AdvancementTools.MALUM_BLACK_CRYSTAL_REVEALED,
                        AdvancementTools.MALUM_BLACK_CRYSTAL_REVEALED_CRITERION);
            }
        });
        context.setPacketHandled(true);
    }
}
