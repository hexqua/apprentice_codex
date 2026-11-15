package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonConfigState;
import org.jetbrains.annotations.NotNull;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncExoskeletonConfigPacket(int lifterManaCost, int striderManaCost) implements CustomPacketPayload {
    public static final Type<SyncExoskeletonConfigPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "sync_exoskeleton_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncExoskeletonConfigPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeVarInt(packet.lifterManaCost); buffer.writeVarInt(packet.striderManaCost); },
            buffer -> new SyncExoskeletonConfigPacket(buffer.readVarInt(), buffer.readVarInt()));

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncExoskeletonConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ExoskeletonConfigState.set(packet.lifterManaCost, packet.striderManaCost));
    }
}
