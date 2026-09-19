package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.magicitem.StorageStabilizer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientOpenStorageStabilizerEnderChestPacket(int sourceSlot) {
    public static void encode(ClientOpenStorageStabilizerEnderChestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.sourceSlot());
    }

    public static ClientOpenStorageStabilizerEnderChestPacket decode(FriendlyByteBuf buffer) {
        return new ClientOpenStorageStabilizerEnderChestPacket(buffer.readVarInt());
    }

    public static void handle(
            ClientOpenStorageStabilizerEnderChestPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender instanceof ServerPlayer && sender.isCreative()) {
                StorageStabilizer.openEnderChestFromInventorySlot(sender, packet.sourceSlot());
            }
        });
        context.setPacketHandled(true);
    }
}
