package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.item.ImmediateSneakSelectionUiItem;
import jp.aquafactory.apprenticecodex.utility.HandStackResolver;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientConfirmSneakSelectionPacket(InteractionHand hand, int selectedIndex) {
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

    public static void handle(ClientConfirmSneakSelectionPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || player.isSpectator() || MagicData.getPlayerMagicData(player).isCasting()) {
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
        context.setPacketHandled(true);
    }
}
