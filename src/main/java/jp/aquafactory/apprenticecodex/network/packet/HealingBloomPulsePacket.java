package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.healingbloom.HealingBloomPulseRenderEvent;
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

public class HealingBloomPulsePacket implements CustomPacketPayload {
    public static final Type<HealingBloomPulsePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "healing_bloom_pulse"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HealingBloomPulsePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), HealingBloomPulsePacket::decode);

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(HealingBloomPulsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeFloat(packet.maxRadius);
    }

    private static HealingBloomPulsePacket decode(FriendlyByteBuf buffer) {
        return new HealingBloomPulsePacket(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat()
        );
    }

    public static void handle(HealingBloomPulsePacket packet, IPayloadContext context) {
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

        private static void handle(HealingBloomPulsePacket packet) {
            HealingBloomPulseRenderEvent.enqueuePulse(new Vec3(packet.x, packet.y, packet.z), packet.maxRadius);
        }
    }
}
