package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientOpenSpellcasterAccessoryCasePacket(int sourceSlot) {
    public static void encode(ClientOpenSpellcasterAccessoryCasePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sourceSlot());
    }

    public static ClientOpenSpellcasterAccessoryCasePacket decode(FriendlyByteBuf buffer) {
        return new ClientOpenSpellcasterAccessoryCasePacket(buffer.readVarInt());
    }

    public static void handle(
            ClientOpenSpellcasterAccessoryCasePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender instanceof ServerPlayer && sender.isCreative()) {
                SpellcasterAccessoryCase.openFromInventorySlot(sender, packet.sourceSlot());
            }
        });
        context.setPacketHandled(true);
    }
}
