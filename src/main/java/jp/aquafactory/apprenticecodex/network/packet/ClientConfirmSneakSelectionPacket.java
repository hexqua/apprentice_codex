package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.ImmediateSneakSelectionUiItem;
import jp.aquafactory.apprenticecodex.utility.HandStackResolver;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientConfirmSneakSelectionPacket(
        InteractionHand hand,
        int selectedIndex
) implements CustomPacketPayload {
    public static final Type<ClientConfirmSneakSelectionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_confirm_sneak_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientConfirmSneakSelectionPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientConfirmSneakSelectionPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientConfirmSneakSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand());
        buffer.writeVarInt(packet.selectedIndex());
    }

    public static ClientConfirmSneakSelectionPacket decode(FriendlyByteBuf buffer) {
        return new ClientConfirmSneakSelectionPacket(
                buffer.readEnum(InteractionHand.class),
                buffer.readVarInt()
        );
    }

    public static void handle(ClientConfirmSneakSelectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
                return;
            }

            var magicData = MagicData.getPlayerMagicData(player);
            if (magicData != null && magicData.isCasting()) {
                return;
            }

            var physicalStack = HandStackResolver.resolve(
                    player,
                    packet.hand(),
                    HandStackResolver.OffhandResolution.PHYSICAL
            );
            if (!(physicalStack.getItem() instanceof ImmediateSneakSelectionUiItem item)) {
                return;
            }
            var stack = item.resolveSneakSelectionStack(player, packet.hand());
            if (stack.getItem() != item || !item.isSneakSelectionUiEnabled(stack)
                    || !item.isSneakSelectionIndexSelectable(stack, packet.selectedIndex())) {
                return;
            }
            item.setSneakSelectionIndex(stack, packet.selectedIndex());
        });
    }
}
