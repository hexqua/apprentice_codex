package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.satellitefollowcastamulet.SatelliteFollowcastAmuletClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncSatelliteFollowcastAmuletStatePacket(
        int ownerEntityId,
        String slotIdentifier,
        int curiosSlotIndex,
        int spellSlotIndex,
        boolean active,
        long activeUntilGameTime
) {
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

    public static void handle(SyncSatelliteFollowcastAmuletStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
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
