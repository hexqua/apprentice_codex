package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientMantleDashInputPacket(long sequence, boolean jump, float forward, float strafe) {
    public static void encode(ClientMantleDashInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.sequence); buffer.writeBoolean(packet.jump); buffer.writeFloat(packet.forward); buffer.writeFloat(packet.strafe);
    }

    public static ClientMantleDashInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientMantleDashInputPacket(buffer.readLong(), buffer.readBoolean(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(ClientMantleDashInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender != null) ShootingStarMantleRuntime.state(sender).elemental.input(sender, packet.sequence, packet.jump, packet.forward, packet.strafe);
        });
        context.setPacketHandled(true);
    }
}
