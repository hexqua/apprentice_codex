package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.anchorblink.AnchorBlinkDaggerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientAnchorBlinkPacket() {
    public static void encode(ClientAnchorBlinkPacket packet, FriendlyByteBuf buffer) {
    }

    public static ClientAnchorBlinkPacket decode(FriendlyByteBuf buffer) {
        return new ClientAnchorBlinkPacket();
    }

    public static void handle(ClientAnchorBlinkPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender != null) {
                AnchorBlinkDaggerEntity.tryBlink(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
