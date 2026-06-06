package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientSwingMagicAttackPacket(boolean bypassChargeCheck, InteractionHand hand) {
    public static void encode(ClientSwingMagicAttackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.bypassChargeCheck());
        buffer.writeEnum(packet.hand());
    }

    public static ClientSwingMagicAttackPacket decode(FriendlyByteBuf buffer) {
        return new ClientSwingMagicAttackPacket(buffer.readBoolean(), buffer.readEnum(InteractionHand.class));
    }

    public static void handle(ClientSwingMagicAttackPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            var stack = sender.getItemInHand(packet.hand());
            if (stack.getItem() instanceof SwingTriggeredMagicItem swingTriggeredMagicItem) {
                swingTriggeredMagicItem.tryTriggerSpellOnSwing(
                        sender,
                        packet.hand(),
                        packet.bypassChargeCheck()
                );
            }
        });
        context.setPacketHandled(true);
    }
}
