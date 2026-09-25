package jp.aquafactory.apprenticecodex.network.packet;

import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.SpellrifleMuzzleParticleHandler;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

public record SpellrifleMuzzleParticlePacket(ClientboundLevelParticlesPacket particle) {

    public static void encode(SpellrifleMuzzleParticlePacket packet, FriendlyByteBuf buffer) {
        packet.particle().write(buffer);
    }

    public static SpellrifleMuzzleParticlePacket decode(FriendlyByteBuf buffer) {
        return new SpellrifleMuzzleParticlePacket(new ClientboundLevelParticlesPacket(buffer));
    }

    public static void handle(SpellrifleMuzzleParticlePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
        private static void handle(SpellrifleMuzzleParticlePacket packet) {
            SpellrifleMuzzleParticleHandler.handle(packet.particle());
        }
    }
}
