package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientMantleFireworkInputPacket(long sequence, boolean jump) {
    public static void encode(ClientMantleFireworkInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.sequence); buffer.writeBoolean(packet.jump);
    }

    public static ClientMantleFireworkInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientMantleFireworkInputPacket(buffer.readLong(), buffer.readBoolean());
    }

    public static void handle(ClientMantleFireworkInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender != null) ShootingStarMantleRuntime.state(sender).firework.input(sender, packet.sequence, packet.jump);
        });
        context.setPacketHandled(true);
    }
}
