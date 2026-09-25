package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientMantleImpulsePacket(long sequence, float forward, float strafe) {
    public static void encode(ClientMantleImpulsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.sequence); buffer.writeFloat(packet.forward); buffer.writeFloat(packet.strafe);
    }

    public static ClientMantleImpulsePacket decode(FriendlyByteBuf buffer) {
        return new ClientMantleImpulsePacket(buffer.readLong(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(ClientMantleImpulsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender != null) ShootingStarMantleRuntime.impulse(sender, packet.sequence, packet.forward, packet.strafe);
        });
        context.setPacketHandled(true);
    }
}
