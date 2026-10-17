package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackEvents;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.UseAction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientPsychicDriveGauntletsUsePacket(UseAction action, InteractionHand hand, long session)
        implements CustomPacketPayload {
    public static final Type<ClientPsychicDriveGauntletsUsePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_psychic_drive_gauntlets_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientPsychicDriveGauntletsUsePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeEnum(packet.action);
                buffer.writeEnum(packet.hand);
                buffer.writeLong(packet.session);
            }, buffer -> new ClientPsychicDriveGauntletsUsePacket(buffer.readEnum(UseAction.class),
                    buffer.readEnum(InteractionHand.class), buffer.readLong()));

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ClientPsychicDriveGauntletsUsePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PsychicDriveGauntletsAttackEvents.useInput(player, packet.action, packet.hand, packet.session);
            }
        });
    }
}
