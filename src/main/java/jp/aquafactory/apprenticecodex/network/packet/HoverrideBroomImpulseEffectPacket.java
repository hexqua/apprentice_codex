package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.renderer.HoverrideBroomImpulseRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HoverrideBroomImpulseEffectPacket(
        double x,
        double y,
        double z,
        float directionX,
        float directionZ
) {
    private static final Vec3 DEFAULT_DIRECTION = new Vec3(0.0D, 0.0D, 1.0D);

    public HoverrideBroomImpulseEffectPacket(Vec3 center, Vec3 direction) {
        this(center.x, center.y, center.z, (float)direction.x, (float)direction.z);
    }

    public HoverrideBroomImpulseEffectPacket {
        x = Double.isFinite(x) ? x : 0.0D;
        y = Double.isFinite(y) ? y : 0.0D;
        z = Double.isFinite(z) ? z : 0.0D;
        var direction = normalizedDirection(directionX, directionZ);
        directionX = (float)direction.x;
        directionZ = (float)direction.z;
    }

    public Vec3 center() {
        return new Vec3(x, y, z);
    }

    public Vec3 direction() {
        return new Vec3(directionX, 0.0D, directionZ);
    }

    public static void encode(HoverrideBroomImpulseEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.directionX);
        buffer.writeFloat(packet.directionZ);
    }

    public static HoverrideBroomImpulseEffectPacket decode(FriendlyByteBuf buffer) {
        return new HoverrideBroomImpulseEffectPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(HoverrideBroomImpulseEffectPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
    }

    private static Vec3 normalizedDirection(float x, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(z)) {
            return DEFAULT_DIRECTION;
        }
        var direction = new Vec3(x, 0.0D, z);
        return direction.lengthSqr() > 1.0e-8D ? direction.normalize() : DEFAULT_DIRECTION;
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(HoverrideBroomImpulseEffectPacket packet) {
            HoverrideBroomImpulseRenderEvent.enqueue(packet.center(), packet.direction());
        }
    }
}
