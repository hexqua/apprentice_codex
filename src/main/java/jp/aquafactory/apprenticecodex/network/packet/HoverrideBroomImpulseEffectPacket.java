package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.renderer.HoverrideBroomImpulseRenderEvent;
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
import org.jetbrains.annotations.NotNull;

public record HoverrideBroomImpulseEffectPacket(
        double x,
        double y,
        double z,
        float directionX,
        float directionZ
) implements CustomPacketPayload {
    private static final Vec3 DEFAULT_DIRECTION = new Vec3(0.0D, 0.0D, 1.0D);

    public static final Type<HoverrideBroomImpulseEffectPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "hoverride_broom_impulse_effect")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, HoverrideBroomImpulseEffectPacket> STREAM_CODEC =
            StreamCodec.of(HoverrideBroomImpulseEffectPacket::encode, HoverrideBroomImpulseEffectPacket::decode);

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

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(FriendlyByteBuf buffer, HoverrideBroomImpulseEffectPacket packet) {
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

    public static void handle(HoverrideBroomImpulseEffectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
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
