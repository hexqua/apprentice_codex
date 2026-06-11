package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.jumpcastcharm.JumpcastCharmCastManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientJumpcastCharmCastPacket() implements CustomPacketPayload {
    public static final Type<ClientJumpcastCharmCastPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_jumpcast_charm_cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientJumpcastCharmCastPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientJumpcastCharmCastPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientJumpcastCharmCastPacket packet, FriendlyByteBuf buffer) {
    }

    public static ClientJumpcastCharmCastPacket decode(FriendlyByteBuf buffer) {
        return new ClientJumpcastCharmCastPacket();
    }

    public static void handle(ClientJumpcastCharmCastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            JumpcastCharmCastManager.tryCast(sender);
        });
    }
}
