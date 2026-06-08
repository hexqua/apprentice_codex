package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.heavenlyfist.HeavenlyFistPulseRenderEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class HeavenlyFistPulsePacket implements CustomPacketPayload {
    public static final Type<HeavenlyFistPulsePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "heavenly_fist_pulse"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HeavenlyFistPulsePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), HeavenlyFistPulsePacket::decode);

    private final double x;
    private final double y;
    private final double z;
    private final float maxRadius;

    public HeavenlyFistPulsePacket(Vec3 center, float maxRadius) {
        this(center.x, center.y, center.z, maxRadius);
    }

    private HeavenlyFistPulsePacket(double x, double y, double z, float maxRadius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxRadius = maxRadius;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(HeavenlyFistPulsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.maxRadius);
    }

    private static HeavenlyFistPulsePacket decode(FriendlyByteBuf buffer) {
        return new HeavenlyFistPulsePacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat()
        );
    }

    public static void handle(HeavenlyFistPulsePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(HeavenlyFistPulsePacket packet) {
            HeavenlyFistPulseRenderEvent.enqueuePulse(new Vec3(packet.x, packet.y, packet.z), packet.maxRadius);
        }
    }
}
