package jp.aquafactory.apprenticecodex.network.packet;

import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifleAdsMovement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record ClientMultipurposeStaffrifleAdsPacket(boolean aiming) {

    public static void encode(ClientMultipurposeStaffrifleAdsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.aiming());
    }

    public static ClientMultipurposeStaffrifleAdsPacket decode(FriendlyByteBuf buffer) {
        return new ClientMultipurposeStaffrifleAdsPacket(buffer.readBoolean());
    }

    public static void handle(ClientMultipurposeStaffrifleAdsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                MultipurposeStaffrifleAdsMovement.update(player, packet.aiming());
            }
        });
        context.setPacketHandled(true);
    }
}
