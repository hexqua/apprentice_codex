package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientBroomInputPacket(
        float strafeInput,
        float forwardInput,
        boolean ascending,
        boolean descending
) {

    public ClientBroomInputPacket {
        strafeInput = sanitizeInput(strafeInput);
        forwardInput = sanitizeInput(forwardInput);
    }

    public static ClientBroomInputPacket inactive() {
        return new ClientBroomInputPacket(0.0F, 0.0F, false, false);
    }

    private static float sanitizeInput(float input) {
        return Float.isFinite(input) ? Mth.clamp(input, -1.0F, 1.0F) : 0.0F;
    }

    public static void encode(ClientBroomInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.strafeInput);
        buffer.writeFloat(packet.forwardInput);
        buffer.writeBoolean(packet.ascending);
        buffer.writeBoolean(packet.descending);
    }

    public static ClientBroomInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientBroomInputPacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(ClientBroomInputPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null
                    || !(sender.getVehicle() instanceof AbstractBroomEntity broom)
                    || broom.getControllingPassenger() != sender) {
                return;
            }
            broom.acceptServerInput(sender, packet.strafeInput, packet.forwardInput,
                    packet.ascending, packet.descending);
        });
        context.setPacketHandled(true);
    }
}
