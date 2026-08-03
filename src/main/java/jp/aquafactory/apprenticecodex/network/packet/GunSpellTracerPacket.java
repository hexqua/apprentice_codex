package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.event.client.GunSpellTracerRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GunSpellTracerPacket {
    private final Vec3 start;
    private final Vec3 end;
    private final float speedBlocksPerTick;
    private final float length;

    public GunSpellTracerPacket(Vec3 start, Vec3 end, float speedBlocksPerTick, float length) {
        this.start = start;
        this.end = end;
        this.speedBlocksPerTick = speedBlocksPerTick;
        this.length = length;
    }

    public static void encode(GunSpellTracerPacket packet, FriendlyByteBuf buffer) {
        writeVec3(buffer, packet.start);
        writeVec3(buffer, packet.end);
        buffer.writeFloat(packet.speedBlocksPerTick);
        buffer.writeFloat(packet.length);
    }

    public static GunSpellTracerPacket decode(FriendlyByteBuf buffer) {
        return new GunSpellTracerPacket(
                readVec3(buffer),
                readVec3(buffer),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    private static void writeVec3(FriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(GunSpellTracerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> ClientHandler.handle(packet));
        context.setPacketHandled(true);
    }

    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(GunSpellTracerPacket packet) {
            GunSpellTracerRenderEvent.enqueueTracer(
                    packet.start,
                    packet.end,
                    packet.speedBlocksPerTick,
                    packet.length
            );
        }
    }
}
