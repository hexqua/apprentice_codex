package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.totemofpermafrost.TotemOfPermafrostPulseRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TotemOfPermafrostPulsePacket {
    private final double x;
    private final double y;
    private final double z;
    private final float maxRadius;

    public TotemOfPermafrostPulsePacket(Vec3 center, float maxRadius) {
        this(center.x, center.y, center.z, maxRadius);
    }

    private TotemOfPermafrostPulsePacket(double x, double y, double z, float maxRadius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxRadius = maxRadius;
    }

    public static void encode(TotemOfPermafrostPulsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.maxRadius);
    }

    public static TotemOfPermafrostPulsePacket decode(FriendlyByteBuf buffer) {
        return new TotemOfPermafrostPulsePacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat()
        );
    }

    public static void handle(TotemOfPermafrostPulsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> ClientHandler.handle(packet));
        context.setPacketHandled(true);
    }

    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(TotemOfPermafrostPulsePacket packet) {
            TotemOfPermafrostPulseRenderEvent.enqueuePulse(new Vec3(packet.x, packet.y, packet.z), packet.maxRadius);
        }
    }
}
