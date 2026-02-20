package jp.aquafactory.apprenticecodex.network;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.packet.SyncEnderGrimoireSpellbookPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Networks {
    private static final String PROTOCOL_VERSION = "1";
    private static int nextPacketId = 0;

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private Networks() {
    }

    public static void register() {
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncEnderGrimoireSpellbookPacket.class,
                SyncEnderGrimoireSpellbookPacket::encode,
                SyncEnderGrimoireSpellbookPacket::decode,
                SyncEnderGrimoireSpellbookPacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer serverPlayer, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
    }
}
