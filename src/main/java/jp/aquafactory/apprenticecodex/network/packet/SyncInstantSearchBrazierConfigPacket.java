package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.magicitem.client.InstantSearchBrazierConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncInstantSearchBrazierConfigPacket(int initialRange) implements CustomPacketPayload {
    public static final Type<SyncInstantSearchBrazierConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_instant_search_brazier_config")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInstantSearchBrazierConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncInstantSearchBrazierConfigPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncInstantSearchBrazierConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.initialRange);
    }

    private static SyncInstantSearchBrazierConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncInstantSearchBrazierConfigPacket(buffer.readVarInt());
    }

    public static void handle(SyncInstantSearchBrazierConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncInstantSearchBrazierConfigPacket packet) {
            InstantSearchBrazierConfigState.set(packet.initialRange);
        }
    }
}
