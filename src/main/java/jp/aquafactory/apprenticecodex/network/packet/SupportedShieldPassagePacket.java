package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.protectionspellsupporter.SupportedShieldPassage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SupportedShieldPassagePacket(ResourceLocation dimension, UUID shield, boolean allowed) implements CustomPacketPayload {
    public static final Type<SupportedShieldPassagePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "supported_shield_passage"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SupportedShieldPassagePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeResourceLocation(packet.dimension);
                buffer.writeUUID(packet.shield);
                buffer.writeBoolean(packet.allowed);
            }, buffer -> new SupportedShieldPassagePacket(buffer.readResourceLocation(), buffer.readUUID(), buffer.readBoolean()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SupportedShieldPassagePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player().level();
            if (level.dimension().location().equals(packet.dimension)) {
                SupportedShieldPassage.receive(level, packet.shield, packet.allowed);
            }
        });
    }
}
