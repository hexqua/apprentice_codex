package jp.aquafactory.apprenticecodex.network;

import jp.aquafactory.apprenticecodex.network.packet.ForceFieldDefenseEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncEnderGrimoireSpellbookPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncRemoteEyeStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncScarletThirstHealthPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class Networks {
    private static final String PROTOCOL_VERSION = "5";

    private Networks() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Networks::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(
                SyncEnderGrimoireSpellbookPacket.TYPE,
                SyncEnderGrimoireSpellbookPacket.STREAM_CODEC,
                SyncEnderGrimoireSpellbookPacket::handle
        );
        registrar.playToClient(
                SyncScarletThirstHealthPacket.TYPE,
                SyncScarletThirstHealthPacket.STREAM_CODEC,
                SyncScarletThirstHealthPacket::handle
        );
        registrar.playToClient(
                ForceFieldDefenseEffectPacket.TYPE,
                ForceFieldDefenseEffectPacket.STREAM_CODEC,
                ForceFieldDefenseEffectPacket::handle
        );
        registrar.playToClient(
                SyncRemoteEyeStatePacket.TYPE,
                SyncRemoteEyeStatePacket.STREAM_CODEC,
                SyncRemoteEyeStatePacket::handle
        );
        registrar.playToClient(
                SenseEvilHighlightsPacket.TYPE,
                SenseEvilHighlightsPacket.STREAM_CODEC,
                SenseEvilHighlightsPacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer serverPlayer, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(serverPlayer, packet);
    }

    public static void sendToTrackingEntityAndSelf(Entity entity, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet);
    }
}
