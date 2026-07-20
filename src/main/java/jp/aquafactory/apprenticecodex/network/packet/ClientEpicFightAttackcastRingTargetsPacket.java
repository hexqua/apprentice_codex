package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record ClientEpicFightAttackcastRingTargetsPacket(List<BlockTargetData> ringTargets)
        implements CustomPacketPayload {
    private static final int MAX_RING_TARGETS = 64;
    public static final Type<ClientEpicFightAttackcastRingTargetsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_epic_fight_attackcast_ring_targets")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientEpicFightAttackcastRingTargetsPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> encode(packet, buffer),
                    ClientEpicFightAttackcastRingTargetsPacket::decode
            );

    public ClientEpicFightAttackcastRingTargetsPacket {
        ringTargets = ringTargets == null ? List.of() : List.copyOf(ringTargets);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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

    public static void handle(ClientEpicFightAttackcastRingTargetsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            EpicFightCompat.queueAttackcastRingTargets(sender, packet.ringTargets());
        });
    }
}
