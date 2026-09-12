package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
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

public record LightningArrowImpactPacket(Vec3 position, Vec3 incoming, boolean blockHit) implements CustomPacketPayload {
    public static final Type<LightningArrowImpactPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "lightning_arrow_impact"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LightningArrowImpactPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVec3(packet.position());
                buffer.writeVec3(packet.incoming());
                buffer.writeBoolean(packet.blockHit());
            }, buffer -> new LightningArrowImpactPacket(buffer.readVec3(), buffer.readVec3(), buffer.readBoolean()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LightningArrowImpactPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(LightningArrowImpactPacket packet) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) return;
            if (!packet.blockHit()) {
                jp.aquafactory.apprenticecodex.spell.shock.ShockImpactParticles.spawn(level, packet.position(),
                        packet.incoming(), jp.aquafactory.apprenticecodex.spell.shock.ShockImpactParticles.Palette.SHOCK);
            } else {
                var position = packet.position().subtract(packet.incoming().scale(0.03));
                for (int i = 0; i < 3; i++) {
                    var velocity = packet.incoming().scale(-0.04).add(
                            (level.random.nextDouble() - 0.5) * 0.05,
                            (level.random.nextDouble() - 0.5) * 0.05,
                            (level.random.nextDouble() - 0.5) * 0.05);
                    var options = new jp.aquafactory.apprenticecodex.particle.AdditiveGlowParticleOptions(
                            jp.aquafactory.apprenticecodex.registry.ParticleRegistry.ADDITIVE_SPARK.get(),
                            0.07F, 0.42F, 0.86F, 1.0F, 2, 5, 1, 0.9F, 1.15F, 0.86F,
                            1.0F, 0.02F, 0.4F, 0.52F, true);
                    level.addParticle(options, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
                }
            }
        }
    }
}
