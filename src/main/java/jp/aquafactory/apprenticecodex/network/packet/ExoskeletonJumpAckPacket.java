package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.ExoskeletonClientEvents;
import org.jetbrains.annotations.NotNull;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExoskeletonJumpAckPacket(long sequence, int mana) implements CustomPacketPayload {
    public static final Type<ExoskeletonJumpAckPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "exoskeleton_jump_ack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExoskeletonJumpAckPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeVarLong(packet.sequence); buffer.writeVarInt(packet.mana); },
            buffer -> new ExoskeletonJumpAckPacket(buffer.readVarLong(), buffer.readVarInt()));

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ExoskeletonJumpAckPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ExoskeletonClientEvents.acknowledge(packet.sequence, packet.mana));
    }
}
