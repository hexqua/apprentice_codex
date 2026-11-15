package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonAction;
import jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton.ExoskeletonMovement;
import jp.aquafactory.apprenticecodex.network.Networks;
import org.jetbrains.annotations.NotNull;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientExoskeletonJumpPacket(long sequence, ExoskeletonAction action) implements CustomPacketPayload {
    public static final Type<ClientExoskeletonJumpPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            ApprenticeCodex.MODID, "exoskeleton_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientExoskeletonJumpPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeVarLong(packet.sequence); buffer.writeEnum(packet.action); },
            buffer -> new ClientExoskeletonJumpPacket(buffer.readVarLong(), buffer.readEnum(ExoskeletonAction.class)));

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ClientExoskeletonJumpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
            boolean accepted = ExoskeletonMovement.accept(player, packet.action);
            // 拒否時も予約を解放するが、本人の予測移動は巻き戻さない。
            Networks.sendToPlayer(player, new ExoskeletonJumpAckPacket(packet.sequence,
                    (int) MagicData.getPlayerMagicData(player).getMana()));
            if (accepted) PacketDistributor.sendToPlayersTrackingEntity(player,
                    new ExoskeletonJumpEffectPacket(player.getId(), packet.action));
        });
    }
}
