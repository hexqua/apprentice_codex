package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomDismountEvents;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientFloatmountBroomDismountInputPacket(int broomId, boolean pressed) {
    public static void encode(ClientFloatmountBroomDismountInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.broomId);
        buffer.writeBoolean(packet.pressed);
    }

    public static ClientFloatmountBroomDismountInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientFloatmountBroomDismountInputPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(ClientFloatmountBroomDismountInputPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null
                    || !(sender.getVehicle() instanceof FloatmountBroomEntity broom)
                    || broom.getId() != packet.broomId
                    || broom.getControllingPassenger() != sender) {
                return;
            }
            FloatmountBroomDismountEvents.handleSneakInput(sender, broom, packet.pressed);
        });
        context.setPacketHandled(true);
    }
}
