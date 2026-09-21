package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoEchoConfigState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncFullautoEchoConfigPacket(boolean enabled, double manaMultiplier, int cooldownBypassThresholdTicks,
                                           int cooldownReductionTicks, int reducedCooldownMinimumTicks) implements CustomPacketPayload {
    public static final Type<SyncFullautoEchoConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_fullauto_echo_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFullautoEchoConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeBoolean(packet.enabled());
                buffer.writeDouble(packet.manaMultiplier());
                buffer.writeVarInt(packet.cooldownBypassThresholdTicks());
                buffer.writeVarInt(packet.cooldownReductionTicks());
                buffer.writeVarInt(packet.reducedCooldownMinimumTicks());
            }, buffer -> new SyncFullautoEchoConfigPacket(buffer.readBoolean(), buffer.readDouble(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncFullautoEchoConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> FullautoEchoConfigState.set(packet.enabled(), packet.manaMultiplier(),
                packet.cooldownBypassThresholdTicks(), packet.cooldownReductionTicks(), packet.reducedCooldownMinimumTicks()));
    }
}
