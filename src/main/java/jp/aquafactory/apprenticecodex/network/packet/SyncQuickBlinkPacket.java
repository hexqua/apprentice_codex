package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.quickblink.QuickBlinkClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncQuickBlinkPacket(int entityId, long start, long sequence, Vec3 direction) {
    public static void encode(SyncQuickBlinkPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeLong(packet.start);
        buffer.writeLong(packet.sequence);
        buffer.writeDouble(packet.direction.x);
        buffer.writeDouble(packet.direction.y);
        buffer.writeDouble(packet.direction.z);
    }

    public static SyncQuickBlinkPacket decode(FriendlyByteBuf buffer) {
        return new SyncQuickBlinkPacket(buffer.readVarInt(), buffer.readLong(), buffer.readLong(),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(SyncQuickBlinkPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncQuickBlinkPacket packet) {
            QuickBlinkClient.accept(packet);
        }
    }
}
