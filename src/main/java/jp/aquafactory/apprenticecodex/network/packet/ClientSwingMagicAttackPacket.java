package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffAttackContextManager;
import jp.aquafactory.apprenticecodex.item.curios.attackcastring.AttackcastRingAttackTrigger;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ClientSwingMagicAttackPacket(boolean bypassChargeCheck, InteractionHand hand, int missEvaluationDelayTicks,
                                           List<BlockTargetData> ringTargets) {
    private static final int MAX_RING_TARGETS = 64;

    public ClientSwingMagicAttackPacket {
        ringTargets = ringTargets == null ? List.of() : List.copyOf(ringTargets);
    }

    public static void encode(ClientSwingMagicAttackPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.bypassChargeCheck());
        buffer.writeEnum(packet.hand());
        buffer.writeVarInt(packet.missEvaluationDelayTicks());
        buffer.writeVarInt(packet.ringTargets().size());
        packet.ringTargets().forEach(target -> target.writeToBuffer(buffer));
    }

    public static ClientSwingMagicAttackPacket decode(FriendlyByteBuf buffer) {
        var bypassChargeCheck = buffer.readBoolean();
        var hand = buffer.readEnum(InteractionHand.class);
        var missEvaluationDelayTicks = buffer.readVarInt();
        var targetCount = buffer.readVarInt();
        if (targetCount < 0 || targetCount > MAX_RING_TARGETS) {
            throw new IllegalArgumentException("Invalid Attackcast Ring target count: " + targetCount);
        }
        var ringTargets = new ArrayList<BlockTargetData>(targetCount);
        for (var index = 0; index < targetCount; ++index) {
            var targetData = new BlockTargetData();
            targetData.readFromBuffer(buffer);
            ringTargets.add(targetData);
        }
        return new ClientSwingMagicAttackPacket(bypassChargeCheck, hand, missEvaluationDelayTicks, ringTargets);
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
                        packet.missEvaluationDelayTicks(),
                        packet.ringTargets()
                );
                return;
            }

            AttackcastRingAttackTrigger.tryTriggerAttack(
                    sender,
                    packet.hand(),
                    packet.bypassChargeCheck(),
                    packet.ringTargets()
            );
        });
        context.setPacketHandled(true);
    }
}
