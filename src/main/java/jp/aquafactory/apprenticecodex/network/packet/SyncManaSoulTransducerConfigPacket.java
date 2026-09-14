package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.manasoultransducer.ManaSoulTransducerConfigState;
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

public record SyncManaSoulTransducerConfigPacket(double castRate, double recoveryRate) implements CustomPacketPayload {
    public static final Type<SyncManaSoulTransducerConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mana_soul_transducer_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaSoulTransducerConfigPacket> STREAM_CODEC =
            StreamCodec.of(SyncManaSoulTransducerConfigPacket::encode, SyncManaSoulTransducerConfigPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buffer, SyncManaSoulTransducerConfigPacket packet) {
        buffer.writeDouble(packet.castRate);
        buffer.writeDouble(packet.recoveryRate);
    }

    private static SyncManaSoulTransducerConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaSoulTransducerConfigPacket(buffer.readDouble(), buffer.readDouble());
    }

    public static void handle(SyncManaSoulTransducerConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncManaSoulTransducerConfigPacket packet) {
            ManaSoulTransducerConfigState.set(packet.castRate, packet.recoveryRate);
        }
    }
}
