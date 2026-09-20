package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCharge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public record ClientInterruptCartridgeReloadPacket() {
    public static void encode(ClientInterruptCartridgeReloadPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {}
    public static ClientInterruptCartridgeReloadPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
        return new ClientInterruptCartridgeReloadPacket();
    }

    public static void handle(ClientInterruptCartridgeReloadPacket packet, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) QuickcastCartridgeCharge.interruptReload(player);
        });
    }
}
