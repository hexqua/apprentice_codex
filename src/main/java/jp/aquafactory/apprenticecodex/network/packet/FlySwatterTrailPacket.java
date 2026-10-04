package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterTrajectory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCurve;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FlySwatterTrailPacket(LockOnRayCurve curve) {
    private static final double PARTICLE_INTERVAL = 0.35;
    private static final int MAX_PARTICLES = 256;
    public static void encode(FlySwatterTrailPacket packet, FriendlyByteBuf buffer) {
        writeVec3(buffer, packet.curve.start());
        writeVec3(buffer, packet.curve.end());
        writeVec3(buffer, packet.curve.startTangent());
        writeVec3(buffer, packet.curve.endTangent());
    }

    public static FlySwatterTrailPacket decode(FriendlyByteBuf buffer) {
        return new FlySwatterTrailPacket(new LockOnRayCurve(
                readVec3(buffer), readVec3(buffer), readVec3(buffer), readVec3(buffer)));
    }

    private static void writeVec3(FriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }

    private static Vec3 readVec3(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(FlySwatterTrailPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet.curve);
        });
        context.setPacketHandled(true);
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
