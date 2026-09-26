package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.event.client.ShootingStarMantleClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncMantlePacket(int entityId, boolean equipped, int energy, int maxEnergy, boolean recovering,
                              boolean hovering, boolean blink, long sequence, boolean accepted,
                              long blinkStart, long blinkSequence, double blinkHeight, Vec3 blinkDirection) {
    public static void encode(SyncMantlePacket packet, FriendlyByteBuf buffer) {
                buffer.writeVarInt(packet.entityId); buffer.writeBoolean(packet.equipped); buffer.writeVarInt(packet.energy);
                buffer.writeVarInt(packet.maxEnergy);
                buffer.writeBoolean(packet.recovering); buffer.writeBoolean(packet.hovering); buffer.writeBoolean(packet.blink);
                buffer.writeLong(packet.sequence); buffer.writeBoolean(packet.accepted);
                buffer.writeLong(packet.blinkStart); buffer.writeLong(packet.blinkSequence); buffer.writeDouble(packet.blinkHeight);
                buffer.writeDouble(packet.blinkDirection.x);
                buffer.writeDouble(packet.blinkDirection.y);
                buffer.writeDouble(packet.blinkDirection.z);
    }

    public static SyncMantlePacket decode(FriendlyByteBuf buffer) {
        return new SyncMantlePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readLong(), buffer.readBoolean(),
                buffer.readLong(), buffer.readLong(), buffer.readDouble(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(SyncMantlePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncMantlePacket packet) { ShootingStarMantleClient.accept(packet); }
    }
}
