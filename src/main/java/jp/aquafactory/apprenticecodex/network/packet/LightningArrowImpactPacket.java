package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions;
import jp.aquafactory.apprenticecodex.registry.ParticleRegistry;
import jp.aquafactory.apprenticecodex.spell.shock.ShockImpactParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record LightningArrowImpactPacket(Vec3 position, Vec3 incoming, boolean blockHit) {
    public static void encode(LightningArrowImpactPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.position().x).writeDouble(packet.position().y).writeDouble(packet.position().z);
        buffer.writeDouble(packet.incoming().x).writeDouble(packet.incoming().y).writeDouble(packet.incoming().z);
        buffer.writeBoolean(packet.blockHit());
    }

    public static LightningArrowImpactPacket decode(FriendlyByteBuf buffer) {
        return new LightningArrowImpactPacket(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readBoolean());
    }

    public static void handle(LightningArrowImpactPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(LightningArrowImpactPacket packet) {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            if (!packet.blockHit()) {
                ShockImpactParticles.spawn(level, packet.position(),
                        packet.incoming(), ShockImpactParticles.Palette.SHOCK);
            } else {
                var position = packet.position().subtract(packet.incoming().scale(0.03));
                for (int i = 0; i < 3; i++) {
                    var velocity = packet.incoming().scale(-0.04).add(
                            (level.random.nextDouble() - 0.5) * 0.05,
                            (level.random.nextDouble() - 0.5) * 0.05,
                            (level.random.nextDouble() - 0.5) * 0.05);
                    var options = new AdditiveGlowParticleOptions(
                            ParticleRegistry.ADDITIVE_SPARK.get(),
                            0.07F, 0.42F, 0.86F, 1.0F, 2, 5, 1, 0.9F, 1.15F, 0.86F,
                            1.0F, 0.02F, 0.4F, 0.52F, true);
                    level.addParticle(options, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
                }
            }
        }
    }
}
