package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.SpellMovementInput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientSpellMovementInputPacket(float forward, float strafe) implements CustomPacketPayload {
    public static final Type<ClientSpellMovementInputPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "spell_movement_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientSpellMovementInputPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeFloat(packet.forward);
                buffer.writeFloat(packet.strafe);
            },
            buffer -> new ClientSpellMovementInputPacket(buffer.readFloat(), buffer.readFloat()));

    @Override
    public @NotNull Type<ClientSpellMovementInputPacket> type() {
        return TYPE;
    }

    public static void handle(ClientSpellMovementInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SpellMovementInput.update(player, packet.forward, packet.strafe);
            }
        });
    }
}
