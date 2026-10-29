package jp.aquafactory.apprenticecodex.network.packet;

import java.util.function.Supplier;
import jp.aquafactory.apprenticecodex.compat.ironsartifice.IronsArtificeCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ClientManaStabilizerInputPacket(boolean aiming, boolean hook, long shot) {
    public static void encode(ClientManaStabilizerInputPacket packet, FriendlyByteBuf buffer) { buffer.writeBoolean(packet.aiming()); buffer.writeBoolean(packet.hook()); buffer.writeVarLong(packet.shot()); }
    public static ClientManaStabilizerInputPacket decode(FriendlyByteBuf buffer) { return new ClientManaStabilizerInputPacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readVarLong()); }
    public static void handle(ClientManaStabilizerInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> { var player = context.getSender(); if (player != null && packet.shot() >= 0) IronsArtificeCompat.manaStabilizerInput(player, packet.aiming(), packet.hook(), packet.shot()); });
        context.setPacketHandled(true);
    }
}
