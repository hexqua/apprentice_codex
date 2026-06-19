package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.anchorblink.AnchorBlinkDaggerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientAnchorBlinkPacket() implements CustomPacketPayload {
    public static final Type<ClientAnchorBlinkPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_anchor_blink"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientAnchorBlinkPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientAnchorBlinkPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientAnchorBlinkPacket packet, FriendlyByteBuf buffer) {
    }

    public static ClientAnchorBlinkPacket decode(FriendlyByteBuf buffer) {
        return new ClientAnchorBlinkPacket();
    }

    public static void handle(ClientAnchorBlinkPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender) {
                AnchorBlinkDaggerEntity.tryBlink(sender);
            }
        });
    }
}
