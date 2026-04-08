package jp.aquafactory.apprenticecodex.network;

import jp.aquafactory.apprenticecodex.network.packet.AtelierStationFluidEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientBlockTargetCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientSwingMagicAttackPacket;
import jp.aquafactory.apprenticecodex.network.packet.ForceFieldDefenseEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.HealingBloomPulsePacket;
import jp.aquafactory.apprenticecodex.network.packet.ManaSiphonOrbEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncEnderGrimoireSpellbookPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncIsekaiTravelGuidebookConfigPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncRemoteEyeStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncScarletThirstHealthPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncSchoolAffinityAssignmentsPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import jp.aquafactory.apprenticecodex.network.packet.SyncTamersPocketCountPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class Networks {
    private static final String PROTOCOL_VERSION = "10";

    private Networks() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Networks::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                ClientBlockTargetCastPacket.TYPE,
                ClientBlockTargetCastPacket.STREAM_CODEC,
                ClientBlockTargetCastPacket::handle
        );
        registrar.playToServer(
                ClientSwingMagicAttackPacket.TYPE,
                ClientSwingMagicAttackPacket.STREAM_CODEC,
                ClientSwingMagicAttackPacket::handle
        );
        registrar.playToClient(
                SyncEnderGrimoireSpellbookPacket.TYPE,
                SyncEnderGrimoireSpellbookPacket.STREAM_CODEC,
                SyncEnderGrimoireSpellbookPacket::handle
        );
        registrar.playToClient(
                SyncIsekaiTravelGuidebookConfigPacket.TYPE,
                SyncIsekaiTravelGuidebookConfigPacket.STREAM_CODEC,
                SyncIsekaiTravelGuidebookConfigPacket::handle
        );
        registrar.playToClient(
                SyncScarletThirstHealthPacket.TYPE,
                SyncScarletThirstHealthPacket.STREAM_CODEC,
                SyncScarletThirstHealthPacket::handle
        );
        registrar.playToClient(
                SyncSchoolAffinityAssignmentsPacket.TYPE,
                SyncSchoolAffinityAssignmentsPacket.STREAM_CODEC,
                SyncSchoolAffinityAssignmentsPacket::handle
        );
        registrar.playToClient(
                ForceFieldDefenseEffectPacket.TYPE,
                ForceFieldDefenseEffectPacket.STREAM_CODEC,
                ForceFieldDefenseEffectPacket::handle
        );
        registrar.playToClient(
                HealingBloomPulsePacket.TYPE,
                HealingBloomPulsePacket.STREAM_CODEC,
                HealingBloomPulsePacket::handle
        );
        registrar.playToClient(
                ManaSiphonOrbEffectPacket.TYPE,
                ManaSiphonOrbEffectPacket.STREAM_CODEC,
                ManaSiphonOrbEffectPacket::handle
        );
        registrar.playToClient(
                SyncRemoteEyeStatePacket.TYPE,
                SyncRemoteEyeStatePacket.STREAM_CODEC,
                SyncRemoteEyeStatePacket::handle
        );
        registrar.playToClient(
                SyncTamersPocketCountPacket.TYPE,
                SyncTamersPocketCountPacket.STREAM_CODEC,
                SyncTamersPocketCountPacket::handle
        );
        registrar.playToClient(
                SenseEvilHighlightsPacket.TYPE,
                SenseEvilHighlightsPacket.STREAM_CODEC,
                SenseEvilHighlightsPacket::handle
        );
        registrar.playToClient(
                AtelierStationFluidEffectPacket.TYPE,
                AtelierStationFluidEffectPacket.STREAM_CODEC,
                AtelierStationFluidEffectPacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer serverPlayer, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(serverPlayer, packet);
    }

    public static void sendToTrackingEntityAndSelf(Entity entity, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, CustomPacketPayload packet) {
        var radiusSqr = radius * radius;
        for (var player : level.players()) {
            if (player.distanceToSqr(center) <= radiusSqr) {
                sendToPlayer(player, packet);
            }
        }
    }
}
