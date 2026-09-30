package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterTrajectory;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCurve;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record FlySwatterTrailPacket(LockOnRayCurve curve) implements CustomPacketPayload {
    private static final double PARTICLE_INTERVAL = 0.35;
    private static final int MAX_PARTICLES = 256;
    public static final Type<FlySwatterTrailPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "fly_swatter_trail"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlySwatterTrailPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVec3(packet.curve.start());
                buffer.writeVec3(packet.curve.end());
                buffer.writeVec3(packet.curve.startTangent());
                buffer.writeVec3(packet.curve.endTangent());
            }, buffer -> new FlySwatterTrailPacket(new LockOnRayCurve(
                    buffer.readVec3(), buffer.readVec3(), buffer.readVec3(), buffer.readVec3())));

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(FlySwatterTrailPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet.curve);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(LockOnRayCurve curve) {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            var from = curve.start();
            double untilNext = 0;
            int count = 0;
            for (var sample : FlySwatterTrajectory.samples(curve)) {
                var delta = sample.position().subtract(from);
                double length = delta.length();
                while (untilNext <= length && count < MAX_PARTICLES) {
                    var point = length < 1.0e-12 ? from : from.add(delta.scale(untilNext / length));
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, point.x, point.y, point.z, 0, 0.001, 0);
                    untilNext += PARTICLE_INTERVAL;
                    ++count;
                }
                untilNext -= length;
                from = sample.position();
                if (count >= MAX_PARTICLES) break;
            }
        }
    }
}
