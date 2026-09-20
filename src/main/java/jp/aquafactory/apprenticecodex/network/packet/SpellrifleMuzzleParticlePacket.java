package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.SpellrifleMuzzleParticleHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SpellrifleMuzzleParticlePacket(ClientboundLevelParticlesPacket particle) implements CustomPacketPayload {
    public static final Type<SpellrifleMuzzleParticlePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spellrifle_muzzle_particle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpellrifleMuzzleParticlePacket> STREAM_CODEC =
            ClientboundLevelParticlesPacket.STREAM_CODEC.map(SpellrifleMuzzleParticlePacket::new,
                    SpellrifleMuzzleParticlePacket::particle);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpellrifleMuzzleParticlePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SpellrifleMuzzleParticlePacket packet) {
            SpellrifleMuzzleParticleHandler.handle(packet.particle());
        }
    }
}
