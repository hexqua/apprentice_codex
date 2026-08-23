package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.BroomDismountEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientBroomDismountInputPacket(int broomId, boolean pressed) {
    public static void encode(ClientBroomDismountInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.broomId);
        buffer.writeBoolean(packet.pressed);
    }

    public static ClientBroomDismountInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientBroomDismountInputPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(ClientBroomDismountInputPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null
                    || !(sender.getVehicle() instanceof AbstractBroomEntity broom)
                    || broom.getId() != packet.broomId
                    || broom.getControllingPassenger() != sender) {
                return;
            }
            BroomDismountEvents.handleSneakInput(sender, broom, packet.pressed);
        });
        context.setPacketHandled(true);
    }
}
