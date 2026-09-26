package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.event.client.ShootingStarMantleClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncMantleDashPacket(int entityId, int kind, int level, boolean hover, long end, long revision, Vec3 motion) {
    public static void encode(SyncMantleDashPacket packet, FriendlyByteBuf buffer) {
                buffer.writeVarInt(packet.entityId); buffer.writeVarInt(packet.kind); buffer.writeVarInt(packet.level);
                buffer.writeBoolean(packet.hover); buffer.writeLong(packet.end); buffer.writeLong(packet.revision);
                buffer.writeDouble(packet.motion.x); buffer.writeDouble(packet.motion.y); buffer.writeDouble(packet.motion.z);
    }

    public static SyncMantleDashPacket decode(FriendlyByteBuf buffer) {
        return new SyncMantleDashPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(),
                buffer.readLong(), buffer.readLong(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(SyncMantleDashPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> { if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet); });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncMantleDashPacket packet) { ShootingStarMantleClient.acceptDash(packet); }
    }
}
