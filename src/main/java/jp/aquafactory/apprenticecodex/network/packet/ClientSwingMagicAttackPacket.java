package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.SwingTriggeredMagicItem;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffAttackContextManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientSwingMagicAttackPacket(boolean bypassChargeCheck, InteractionHand hand, int missEvaluationDelayTicks) {
    public static void encode(ClientSwingMagicAttackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.bypassChargeCheck());
        buffer.writeEnum(packet.hand());
        buffer.writeVarInt(packet.missEvaluationDelayTicks());
    }

    public static ClientSwingMagicAttackPacket decode(FriendlyByteBuf buffer) {
        return new ClientSwingMagicAttackPacket(
                buffer.readBoolean(),
                buffer.readEnum(InteractionHand.class),
                buffer.readVarInt()
        );
    }

    public static void handle(ClientSwingMagicAttackPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            var stack = sender.getItemInHand(packet.hand());
            if (CrystalBladedStaff.isCrystalBladedStaff(stack)) {
                CrystalBladedStaffAttackContextManager.requestMissTrigger(
                        sender,
                        packet.hand(),
                        packet.bypassChargeCheck(),
                        packet.missEvaluationDelayTicks()
                );
                return;
            }

            if (stack.getItem() instanceof SwingTriggeredMagicItem swingTriggeredMagicItem
                    && swingTriggeredMagicItem.canTriggerSpellOnSwing(sender, packet.hand())) {
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
