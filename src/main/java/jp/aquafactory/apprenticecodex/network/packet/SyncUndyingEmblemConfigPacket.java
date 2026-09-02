package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.undyingemblem.UndyingEmblemConfigState;
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

public record SyncUndyingEmblemConfigPacket(int reconstructionSpeedMultiplier) implements CustomPacketPayload {
    public static final Type<SyncUndyingEmblemConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_undying_emblem_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncUndyingEmblemConfigPacket> STREAM_CODEC =
            StreamCodec.of(SyncUndyingEmblemConfigPacket::encode, SyncUndyingEmblemConfigPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buffer, SyncUndyingEmblemConfigPacket packet) {
        buffer.writeVarInt(packet.reconstructionSpeedMultiplier);
    }

    private static SyncUndyingEmblemConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncUndyingEmblemConfigPacket(buffer.readVarInt());
    }

    public static void handle(SyncUndyingEmblemConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncUndyingEmblemConfigPacket packet) {
            UndyingEmblemConfigState.set(packet.reconstructionSpeedMultiplier);
        }
    }
}
