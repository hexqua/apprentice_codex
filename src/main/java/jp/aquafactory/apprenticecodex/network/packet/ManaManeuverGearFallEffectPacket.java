package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear.client.ManaManeuverGearFallEffectRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

public record ManaManeuverGearFallEffectPacket(double x, double y, double z,
                                                float maxRadius) {

    public ManaManeuverGearFallEffectPacket(Vec3 center, float maxRadius) {
        this(center.x, center.y, center.z, maxRadius);
    }


    public static void encode(ManaManeuverGearFallEffectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.maxRadius);
    }

    public static ManaManeuverGearFallEffectPacket decode(FriendlyByteBuf buffer) {
        return new ManaManeuverGearFallEffectPacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat()
        );
    }

    public static void handle(ManaManeuverGearFallEffectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(ManaManeuverGearFallEffectPacket packet) {
            ManaManeuverGearFallEffectRenderEvent.enqueuePulse(
                    new Vec3(packet.x, packet.y, packet.z),
                    packet.maxRadius
            );
        }
    }
}
