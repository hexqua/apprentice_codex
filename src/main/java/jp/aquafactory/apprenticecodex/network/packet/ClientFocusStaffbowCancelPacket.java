package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowCastManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientFocusStaffbowCancelPacket() implements CustomPacketPayload {
    public static final Type<ClientFocusStaffbowCancelPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_focus_staffbow_cancel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientFocusStaffbowCancelPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientFocusStaffbowCancelPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientFocusStaffbowCancelPacket packet, FriendlyByteBuf buffer) {
    }

    public static ClientFocusStaffbowCancelPacket decode(FriendlyByteBuf buffer) {
        return new ClientFocusStaffbowCancelPacket();
    }

    public static void handle(ClientFocusStaffbowCancelPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            FocusStaffbowCastManager.cancelPendingCast(sender);
        });
    }
}
