package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.isekaitravelguidebook.IsekaiTravelGuidebookTooltipState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SyncIsekaiTravelGuidebookConfigPacket implements CustomPacketPayload {
    public static final Type<SyncIsekaiTravelGuidebookConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_isekai_travel_guidebook_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncIsekaiTravelGuidebookConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncIsekaiTravelGuidebookConfigPacket::decode);

    private final boolean showTooltip;

    public SyncIsekaiTravelGuidebookConfigPacket(boolean showTooltip) {
        this.showTooltip = showTooltip;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncIsekaiTravelGuidebookConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.showTooltip);
    }

    private static SyncIsekaiTravelGuidebookConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncIsekaiTravelGuidebookConfigPacket(buffer.readBoolean());
    }

    public static void handle(SyncIsekaiTravelGuidebookConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncIsekaiTravelGuidebookConfigPacket packet) {
            IsekaiTravelGuidebookTooltipState.setShowTooltip(packet.showTooltip);
        }
    }
}
