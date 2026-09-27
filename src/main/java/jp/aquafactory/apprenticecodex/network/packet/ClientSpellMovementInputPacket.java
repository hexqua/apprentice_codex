package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.SpellMovementInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientSpellMovementInputPacket(float forward, float strafe) {
    public static void encode(ClientSpellMovementInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.forward);
        buffer.writeFloat(packet.strafe);
    }

    public static ClientSpellMovementInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientSpellMovementInputPacket(buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(ClientSpellMovementInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                SpellMovementInput.update(context.getSender(), packet.forward, packet.strafe);
            }
        });
        context.setPacketHandled(true);
    }
}
