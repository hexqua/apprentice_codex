package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ClientEpicFightAttackcastRingTargetsPacket(List<BlockTargetData> ringTargets) {
    private static final int MAX_RING_TARGETS = 64;

    public ClientEpicFightAttackcastRingTargetsPacket {
        ringTargets = ringTargets == null ? List.of() : List.copyOf(ringTargets);
    }

    public static void encode(ClientEpicFightAttackcastRingTargetsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.ringTargets().size());
        packet.ringTargets().forEach(target -> target.writeToBuffer(buffer));
    }

    public static ClientEpicFightAttackcastRingTargetsPacket decode(FriendlyByteBuf buffer) {
        var targetCount = buffer.readVarInt();
        if (targetCount < 0 || targetCount > MAX_RING_TARGETS) {
            throw new IllegalArgumentException("Invalid Epic Fight Attackcast Ring target count: " + targetCount);
        }

        var ringTargets = new ArrayList<BlockTargetData>(targetCount);
        for (var index = 0; index < targetCount; ++index) {
            var targetData = new BlockTargetData();
            targetData.readFromBuffer(buffer);
            ringTargets.add(targetData);
        }
        return new ClientEpicFightAttackcastRingTargetsPacket(ringTargets);
    }

    public static void handle(
            ClientEpicFightAttackcastRingTargetsPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            EpicFightCompat.queueAttackcastRingTargets(sender, packet.ringTargets());
        });
        context.setPacketHandled(true);
    }
}
