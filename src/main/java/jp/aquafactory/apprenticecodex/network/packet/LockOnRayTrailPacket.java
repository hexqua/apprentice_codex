package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.LockOnRayTrailRenderEvent;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCurve;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record LockOnRayTrailPacket(LockOnRayCurve curve) implements CustomPacketPayload {
    public static final Type<LockOnRayTrailPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "lock_on_ray_trail"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LockOnRayTrailPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVec3(packet.curve.start());
                buffer.writeVec3(packet.curve.end());
                buffer.writeVec3(packet.curve.startTangent());
                buffer.writeVec3(packet.curve.endTangent());
            }, buffer -> new LockOnRayTrailPacket(new LockOnRayCurve(
                    buffer.readVec3(), buffer.readVec3(), buffer.readVec3(), buffer.readVec3())));

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LockOnRayTrailPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(LockOnRayTrailPacket packet) {
            LockOnRayTrailRenderEvent.enqueue(packet.curve);
        }
    }
}
