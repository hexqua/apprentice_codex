package jp.aquafactory.apprenticecodex.network.packet;

import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleAdsMovement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record ClientFullautoRapidcastSpellrifleAdsPacket(boolean aiming) {

    public static void encode(ClientFullautoRapidcastSpellrifleAdsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.aiming());
    }

    public static ClientFullautoRapidcastSpellrifleAdsPacket decode(FriendlyByteBuf buffer) {
        return new ClientFullautoRapidcastSpellrifleAdsPacket(buffer.readBoolean());
    }

    public static void handle(ClientFullautoRapidcastSpellrifleAdsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                FullautoRapidcastSpellrifleAdsMovement.update(player, packet.aiming());
            }
        });
        context.setPacketHandled(true);
    }
}
