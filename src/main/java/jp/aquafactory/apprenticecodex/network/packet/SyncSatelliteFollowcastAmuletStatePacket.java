package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmuletClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncSatelliteFollowcastAmuletStatePacket(
        int ownerEntityId,
        String slotIdentifier,
        int curiosSlotIndex,
        int spellSlotIndex,
        boolean active,
        long activeUntilGameTime
) implements CustomPacketPayload {
    public static final Type<SyncSatelliteFollowcastAmuletStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_satellite_followcast_amulet_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSatelliteFollowcastAmuletStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncSatelliteFollowcastAmuletStatePacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncSatelliteFollowcastAmuletStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.ownerEntityId);
        buffer.writeUtf(packet.slotIdentifier);
        buffer.writeVarInt(packet.curiosSlotIndex);
        buffer.writeVarInt(packet.spellSlotIndex);
        buffer.writeBoolean(packet.active);
        buffer.writeLong(packet.activeUntilGameTime);
    }

    public static SyncSatelliteFollowcastAmuletStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncSatelliteFollowcastAmuletStatePacket(
                buffer.readVarInt(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readLong()
        );
    }

    public static void handle(SyncSatelliteFollowcastAmuletStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncSatelliteFollowcastAmuletStatePacket packet) {
            SatelliteFollowcastAmuletClientState.setContinuousActive(
                    packet.ownerEntityId,
                    packet.slotIdentifier,
                    packet.curiosSlotIndex,
                    packet.spellSlotIndex,
                    packet.active,
                    packet.activeUntilGameTime
            );
        }
    }
}
