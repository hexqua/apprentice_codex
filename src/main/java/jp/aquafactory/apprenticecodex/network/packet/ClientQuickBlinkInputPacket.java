package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.quickblink.QuickBlinkRuntime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientQuickBlinkInputPacket(float forward, float strafe) implements CustomPacketPayload {
    public static final Type<ClientQuickBlinkInputPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "quick_blink_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientQuickBlinkInputPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeFloat(packet.forward); buffer.writeFloat(packet.strafe); },
            buffer -> new ClientQuickBlinkInputPacket(buffer.readFloat(), buffer.readFloat()));

    @Override
    public @NotNull Type<ClientQuickBlinkInputPacket> type() { return TYPE; }

    public static void handle(ClientQuickBlinkInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) QuickBlinkRuntime.input(player, packet.forward, packet.strafe);
        });
    }
}
