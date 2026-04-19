package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.FocusStaffbowCastManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientFocusStaffbowCancelPacket() {
    public static void encode(ClientFocusStaffbowCancelPacket packet, FriendlyByteBuf buffer) {
    }

    public static ClientFocusStaffbowCancelPacket decode(FriendlyByteBuf buffer) {
        return new ClientFocusStaffbowCancelPacket();
    }

    public static void handle(ClientFocusStaffbowCancelPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            FocusStaffbowCastManager.cancelPendingCast(sender);
        });
        context.setPacketHandled(true);
    }
}
