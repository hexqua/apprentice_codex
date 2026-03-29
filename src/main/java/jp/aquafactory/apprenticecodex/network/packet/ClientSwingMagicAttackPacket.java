package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.AbstractSwingMagicItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientSwingMagicAttackPacket() {
    public static void encode(ClientSwingMagicAttackPacket packet, FriendlyByteBuf buffer) {
    }

    public static ClientSwingMagicAttackPacket decode(FriendlyByteBuf buffer) {
        return new ClientSwingMagicAttackPacket();
    }

    public static void handle(ClientSwingMagicAttackPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            var mainHandItem = sender.getMainHandItem().getItem();
            if (mainHandItem instanceof AbstractSwingMagicItem swingMagicItem) {
                swingMagicItem.tryTriggerImbuedSpellOnSwing(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
