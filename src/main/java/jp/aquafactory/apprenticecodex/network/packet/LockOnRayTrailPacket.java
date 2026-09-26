package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.event.client.LockOnRayTrailRenderEvent;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCurve;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record LockOnRayTrailPacket(LockOnRayCurve curve) {
    public static void encode(LockOnRayTrailPacket packet, FriendlyByteBuf buffer) {
        writeVec3(buffer, packet.curve.start());
        writeVec3(buffer, packet.curve.end());
        writeVec3(buffer, packet.curve.startTangent());
        writeVec3(buffer, packet.curve.endTangent());
    }

    public static LockOnRayTrailPacket decode(FriendlyByteBuf buffer) {
        return new LockOnRayTrailPacket(new LockOnRayCurve(
                readVec3(buffer), readVec3(buffer), readVec3(buffer), readVec3(buffer)));
    }

    private static void writeVec3(FriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(LockOnRayTrailPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(LockOnRayTrailPacket packet) {
            LockOnRayTrailRenderEvent.enqueue(packet.curve);
        }
    }
}
