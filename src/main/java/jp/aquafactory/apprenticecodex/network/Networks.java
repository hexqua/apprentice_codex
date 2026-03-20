package jp.aquafactory.apprenticecodex.network;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.packet.AtelierStationFluidEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.ClientBlockTargetCastPacket;
import jp.aquafactory.apprenticecodex.network.packet.ForceFieldDefenseEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.ManaSiphonOrbEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.SenseEvilHighlightsPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncEnderGrimoireSpellbookPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncRemoteEyeStatePacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncScarletThirstHealthPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Networks {
    private static final String PROTOCOL_VERSION = "6";
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
                ClientBlockTargetCastPacket.class,
                ClientBlockTargetCastPacket::encode,
                ClientBlockTargetCastPacket::decode,
                ClientBlockTargetCastPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncEnderGrimoireSpellbookPacket.class,
                SyncEnderGrimoireSpellbookPacket::encode,
                SyncEnderGrimoireSpellbookPacket::decode,
                SyncEnderGrimoireSpellbookPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncScarletThirstHealthPacket.class,
                SyncScarletThirstHealthPacket::encode,
                SyncScarletThirstHealthPacket::decode,
                SyncScarletThirstHealthPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ForceFieldDefenseEffectPacket.class,
                ForceFieldDefenseEffectPacket::encode,
                ForceFieldDefenseEffectPacket::decode,
                ForceFieldDefenseEffectPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ManaSiphonOrbEffectPacket.class,
                ManaSiphonOrbEffectPacket::encode,
                ManaSiphonOrbEffectPacket::decode,
                ManaSiphonOrbEffectPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SyncRemoteEyeStatePacket.class,
                SyncRemoteEyeStatePacket::encode,
                SyncRemoteEyeStatePacket::decode,
                SyncRemoteEyeStatePacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                SenseEvilHighlightsPacket.class,
                SenseEvilHighlightsPacket::encode,
                SenseEvilHighlightsPacket::decode,
                SenseEvilHighlightsPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                AtelierStationFluidEffectPacket.class,
                AtelierStationFluidEffectPacket::encode,
                AtelierStationFluidEffectPacket::decode,
                AtelierStationFluidEffectPacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer serverPlayer, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
    }

    public static void sendToTrackingEntityAndSelf(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }

    public static void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, Object packet) {
        var radiusSqr = radius * radius;
        for (var player : level.players()) {
            if (player.distanceToSqr(center) <= radiusSqr) {
                sendToPlayer(player, packet);
            }
        }
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
