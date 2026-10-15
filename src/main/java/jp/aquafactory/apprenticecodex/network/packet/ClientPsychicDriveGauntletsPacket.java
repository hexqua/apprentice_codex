package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackEvents;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.Input;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientPsychicDriveGauntletsPacket(Input input) implements CustomPacketPayload {
    public static final Type<ClientPsychicDriveGauntletsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_psychic_drive_gauntlets"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientPsychicDriveGauntletsPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> buffer.writeEnum(packet.input), buffer ->
                    new ClientPsychicDriveGauntletsPacket(buffer.readEnum(Input.class)));
    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(ClientPsychicDriveGauntletsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) PsychicDriveGauntletsAttackEvents.input(player, packet.input);
        });
    }
}
