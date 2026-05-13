package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.SmashcastScepterClientRenderState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SyncSmashcastScepterReadyStatePacket implements CustomPacketPayload {
    public static final Type<SyncSmashcastScepterReadyStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(jp.aquafactory.apprenticecodex.ApprenticeCodex.MODID, "sync_smashcast_scepter_ready_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSmashcastScepterReadyStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncSmashcastScepterReadyStatePacket::decode);

    private final boolean ready;

    public SyncSmashcastScepterReadyStatePacket(boolean ready) {
        this.ready = ready;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncSmashcastScepterReadyStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.ready);
    }

    private static SyncSmashcastScepterReadyStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncSmashcastScepterReadyStatePacket(buffer.readBoolean());
    }

    public static void handle(SyncSmashcastScepterReadyStatePacket packet, IPayloadContext context) {
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

        private static void handle(SyncSmashcastScepterReadyStatePacket packet) {
            SmashcastScepterClientRenderState.setSyncedReadyState(packet.ready);
        }
    }
}
