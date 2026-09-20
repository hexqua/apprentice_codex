package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowPendingCast;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public record ClientElementalBowCancelPacket() {
    public static void encode(ClientElementalBowCancelPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {}
    public static ClientElementalBowCancelPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
        return new ClientElementalBowCancelPacket();
    }

    public static void handle(ClientElementalBowCancelPacket packet, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) ElementalBowPendingCast.cancel(player);
        });
    }
}
