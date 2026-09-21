package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCharge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientInterruptCartridgeReloadPacket() {
    public static void encode(ClientInterruptCartridgeReloadPacket packet, FriendlyByteBuf buffer) {}
    public static ClientInterruptCartridgeReloadPacket decode(FriendlyByteBuf buffer) {
        return new ClientInterruptCartridgeReloadPacket();
    }

    public static void handle(ClientInterruptCartridgeReloadPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) QuickcastCartridgeCharge.interruptReload(player);
        });
    }
}
