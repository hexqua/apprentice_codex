package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleAdsMovement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientMultipurposeStaffrifleAdsPacket(boolean aiming) implements CustomPacketPayload {
    public static final Type<ClientMultipurposeStaffrifleAdsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_multipurpose_staffrifle_ads"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMultipurposeStaffrifleAdsPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> buffer.writeBoolean(packet.aiming()),
                    buffer -> new ClientMultipurposeStaffrifleAdsPacket(buffer.readBoolean()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientMultipurposeStaffrifleAdsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MultipurposeStaffrifleAdsMovement.update(player, packet.aiming());
            }
        });
    }
}
