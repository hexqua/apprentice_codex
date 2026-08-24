package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.spellcasteraccessorycase.SpellcasterAccessoryCaseMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientConfigureSpellcasterAccessoryCaseMenuPacket(
        int containerId,
        int maxVisibleCuriosColumns
) {
    public static void encode(ClientConfigureSpellcasterAccessoryCaseMenuPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId());
        buffer.writeVarInt(packet.maxVisibleCuriosColumns());
    }

    public static ClientConfigureSpellcasterAccessoryCaseMenuPacket decode(FriendlyByteBuf buffer) {
        return new ClientConfigureSpellcasterAccessoryCaseMenuPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(
            ClientConfigureSpellcasterAccessoryCaseMenuPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null
                    || packet.maxVisibleCuriosColumns() < 0
                    || !(sender.containerMenu instanceof SpellcasterAccessoryCaseMenu menu)
                    || menu.containerId != packet.containerId()) {
                return;
            }

            // clientは表示許容量だけを選び、実際のslot数と全inventory移動はserver側menuが決定する。
            menu.configureMaxVisibleCuriosColumns(packet.maxVisibleCuriosColumns());
        });
        context.setPacketHandled(true);
    }
}
