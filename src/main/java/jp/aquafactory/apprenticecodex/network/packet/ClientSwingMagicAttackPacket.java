package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaff;
import jp.aquafactory.apprenticecodex.item.crystalbladedstaff.CrystalBladedStaffAttackContextManager;
import jp.aquafactory.apprenticecodex.item.curios.attackcastring.AttackcastRingAttackTrigger;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record ClientSwingMagicAttackPacket(
        boolean bypassChargeCheck,
        InteractionHand hand,
        int missEvaluationDelayTicks,
        List<BlockTargetData> ringTargets
) implements CustomPacketPayload {
    private static final int MAX_RING_TARGETS = 64;
    public static final Type<ClientSwingMagicAttackPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_swing_magic_attack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientSwingMagicAttackPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientSwingMagicAttackPacket::decode);

    public ClientSwingMagicAttackPacket {
        ringTargets = ringTargets == null ? List.of() : List.copyOf(ringTargets);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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

    public static void handle(ClientSwingMagicAttackPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
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
    }
}
