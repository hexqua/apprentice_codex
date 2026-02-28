package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.event.ForceFieldDefenseEffectRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ForceFieldDefenseEffectPacket {
    private final double x;
    private final double y;
    private final double z;
    private final float normalX;
    private final float normalY;
    private final float normalZ;

    public ForceFieldDefenseEffectPacket(Vec3 position, Vec3 normal) {
        this(position.x, position.y, position.z, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private ForceFieldDefenseEffectPacket(double x, double y, double z, float normalX, float normalY, float normalZ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
    }

    public static void encode(ForceFieldDefenseEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.normalX);
        buffer.writeFloat(packet.normalY);
        buffer.writeFloat(packet.normalZ);
    }

    public static ForceFieldDefenseEffectPacket decode(FriendlyByteBuf buffer) {
        return new ForceFieldDefenseEffectPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(ForceFieldDefenseEffectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(ForceFieldDefenseEffectPacket packet) {
            ForceFieldDefenseEffectRenderEvent.enqueueEffect(
                    new Vec3(packet.x, packet.y, packet.z),
                    new Vec3(packet.normalX, packet.normalY, packet.normalZ)
            );
        }
    }
}
