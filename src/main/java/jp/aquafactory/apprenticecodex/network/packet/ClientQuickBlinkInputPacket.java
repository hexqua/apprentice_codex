package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.quickblink.QuickBlinkRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientQuickBlinkInputPacket(float forward, float strafe) {
    public static void encode(ClientQuickBlinkInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.forward);
        buffer.writeFloat(packet.strafe);
    }

    public static ClientQuickBlinkInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientQuickBlinkInputPacket(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(ClientQuickBlinkInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                QuickBlinkRuntime.input(context.getSender(), packet.forward, packet.strafe);
            }
        });
        context.setPacketHandled(true);
    }
}
