package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterFlightManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientManaThrusterInputPacket(boolean active) {
    public static void encode(ClientManaThrusterInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
    }

    public static ClientManaThrusterInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientManaThrusterInputPacket(buffer.readBoolean());
    }

    public static void handle(ClientManaThrusterInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null) {
                return;
            }

            ManaThrusterFlightManager.setJumpInput(sender, packet.active);
        });
        context.setPacketHandled(true);
    }
}
