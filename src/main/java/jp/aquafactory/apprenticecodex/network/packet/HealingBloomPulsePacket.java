package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomPulseRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HealingBloomPulsePacket {
    private final double x;
    private final double y;
    private final double z;
    private final float maxRadius;

    public HealingBloomPulsePacket(Vec3 center, float maxRadius) {
        this(center.x, center.y, center.z, maxRadius);
    }

    private HealingBloomPulsePacket(double x, double y, double z, float maxRadius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxRadius = maxRadius;
    }

    public static void encode(HealingBloomPulsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.maxRadius);
    }

    public static HealingBloomPulsePacket decode(FriendlyByteBuf buffer) {
        return new HealingBloomPulsePacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat()
        );
    }

    public static void handle(HealingBloomPulsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(HealingBloomPulsePacket packet) {
            HealingBloomPulseRenderEvent.enqueuePulse(new Vec3(packet.x, packet.y, packet.z), packet.maxRadius);
        }
    }
}
