package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.undyingemblem.UndyingEmblemClientState;
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

public record SyncUndyingEmblemStatePacket(int remainingCooldownTicks, long serverGameTime)
        implements CustomPacketPayload {
    public static final Type<SyncUndyingEmblemStatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_undying_emblem_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncUndyingEmblemStatePacket> STREAM_CODEC =
            StreamCodec.of(SyncUndyingEmblemStatePacket::encode, SyncUndyingEmblemStatePacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buffer, SyncUndyingEmblemStatePacket packet) {
        buffer.writeVarInt(packet.remainingCooldownTicks);
        buffer.writeLong(packet.serverGameTime);
    }

    private static SyncUndyingEmblemStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncUndyingEmblemStatePacket(buffer.readVarInt(), buffer.readLong());
    }

    public static void handle(SyncUndyingEmblemStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncUndyingEmblemStatePacket packet) {
            UndyingEmblemClientState.set(packet.remainingCooldownTicks, packet.serverGameTime);
        }
    }
}
