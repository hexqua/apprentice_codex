package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientFloatmountBroomInputPacket(
        float strafeInput,
        float forwardInput,
        boolean ascending,
        boolean descending
) {

    public ClientFloatmountBroomInputPacket {
        strafeInput = sanitizeInput(strafeInput);
        forwardInput = sanitizeInput(forwardInput);
    }

    public static ClientFloatmountBroomInputPacket inactive() {
        return new ClientFloatmountBroomInputPacket(0.0F, 0.0F, false, false);
    }

    private static float sanitizeInput(float input) {
        return Float.isFinite(input) ? Mth.clamp(input, -1.0F, 1.0F) : 0.0F;
    }

    public static void encode(ClientFloatmountBroomInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.strafeInput);
        buffer.writeFloat(packet.forwardInput);
        buffer.writeBoolean(packet.ascending);
        buffer.writeBoolean(packet.descending);
    }

    public static ClientFloatmountBroomInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientFloatmountBroomInputPacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(ClientFloatmountBroomInputPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null
                    || !(sender.getVehicle() instanceof FloatmountBroomEntity broom)
                    || broom.getControllingPassenger() != sender) {
                return;
            }
            broom.acceptServerInput(sender, packet.strafeInput, packet.forwardInput,
                    packet.ascending, packet.descending);
        });
        context.setPacketHandled(true);
    }
}
