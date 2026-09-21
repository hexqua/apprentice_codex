package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowPendingCast;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientElementalBowCancelPacket() {
    public static void encode(ClientElementalBowCancelPacket packet, FriendlyByteBuf buffer) {}
    public static ClientElementalBowCancelPacket decode(FriendlyByteBuf buffer) {
        return new ClientElementalBowCancelPacket();
    }

    public static void handle(ClientElementalBowCancelPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) ElementalBowPendingCast.cancel(player);
        });
    }
}
