package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.magelight.MageLightConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncMageLightConfigPacket(double maxRange) implements CustomPacketPayload {
    public static final Type<SyncMageLightConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mage_light_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMageLightConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncMageLightConfigPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void encode(SyncMageLightConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.maxRange);
    }

    public static SyncMageLightConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncMageLightConfigPacket(buffer.readDouble());
    }

    public static void handle(SyncMageLightConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncMageLightConfigPacket packet) {
            MageLightConfigState.set(packet.maxRange);
        }
    }
}
