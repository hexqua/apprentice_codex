package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.terraresonance.TerraResonancePulseRenderEvent;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TerraResonancePulsePacket {
    private final double x;
    private final double y;
    private final double z;
    private final Direction selectedFace;
    private final float maxRadius;

    public TerraResonancePulsePacket(Vec3 center, Direction selectedFace, float maxRadius) {
        this(center.x, center.y, center.z, selectedFace, maxRadius);
    }

    private TerraResonancePulsePacket(double x, double y, double z, Direction selectedFace, float maxRadius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.selectedFace = selectedFace;
        this.maxRadius = maxRadius;
    }

    public static void encode(TerraResonancePulsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeEnum(packet.selectedFace);
        buffer.writeFloat(packet.maxRadius);
    }

    public static TerraResonancePulsePacket decode(FriendlyByteBuf buffer) {
        return new TerraResonancePulsePacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readEnum(Direction.class),
                buffer.readFloat()
        );
    }

    public static void handle(TerraResonancePulsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(TerraResonancePulsePacket packet) {
            TerraResonancePulseRenderEvent.enqueuePulse(
                    new Vec3(packet.x, packet.y, packet.z),
                    packet.selectedFace,
                    packet.maxRadius
            );
        }
    }
}
