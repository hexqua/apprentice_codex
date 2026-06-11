package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm.JumpcastCharmCastManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientJumpcastCharmCastPacket() {
    public static void encode(ClientJumpcastCharmCastPacket packet, FriendlyByteBuf buffer) {
    }

    public static ClientJumpcastCharmCastPacket decode(FriendlyByteBuf buffer) {
        return new ClientJumpcastCharmCastPacket();
    }

    public static void handle(ClientJumpcastCharmCastPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null) {
                return;
            }

            JumpcastCharmCastManager.tryCast(sender);
        });
        context.setPacketHandled(true);
    }
}
