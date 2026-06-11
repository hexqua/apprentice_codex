package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterFlightManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientManaThrusterInputPacket(boolean active, float strafeInput, float forwardInput) {
    public ClientManaThrusterInputPacket {
        strafeInput = active ? sanitizeInput(strafeInput) : 0.0F;
        forwardInput = active ? sanitizeInput(forwardInput) : 0.0F;
    }

    public static ClientManaThrusterInputPacket inactive() {
        return new ClientManaThrusterInputPacket(false, 0.0F, 0.0F);
    }

    private static float sanitizeInput(float input) {
        return Float.isFinite(input) ? Mth.clamp(input, -1.0F, 1.0F) : 0.0F;
    }

    public static void encode(ClientManaThrusterInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeFloat(packet.strafeInput);
        buffer.writeFloat(packet.forwardInput);
    }

    public static ClientManaThrusterInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientManaThrusterInputPacket(buffer.readBoolean(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(ClientManaThrusterInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null) {
                return;
            }

            ManaThrusterFlightManager.setJumpInput(sender, packet.active, packet.strafeInput, packet.forwardInput);
        });
        context.setPacketHandled(true);
    }
}
