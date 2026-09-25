package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoEchoConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncFullautoEchoConfigPacket(boolean enabled, double manaMultiplier, int cooldownBypassThresholdTicks,
                                           int cooldownReductionTicks, int reducedCooldownMinimumTicks) {
    public static void encode(SyncFullautoEchoConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.enabled());
        buffer.writeDouble(packet.manaMultiplier());
        buffer.writeVarInt(packet.cooldownBypassThresholdTicks());
        buffer.writeVarInt(packet.cooldownReductionTicks());
        buffer.writeVarInt(packet.reducedCooldownMinimumTicks());
    }

    public static SyncFullautoEchoConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncFullautoEchoConfigPacket(buffer.readBoolean(), buffer.readDouble(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SyncFullautoEchoConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> FullautoEchoConfigState.set(packet.enabled(), packet.manaMultiplier(),
                packet.cooldownBypassThresholdTicks(), packet.cooldownReductionTicks(), packet.reducedCooldownMinimumTicks()));
        context.setPacketHandled(true);
    }
}
