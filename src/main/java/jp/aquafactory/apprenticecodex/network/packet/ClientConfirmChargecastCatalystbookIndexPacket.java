package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatOffhandAttributeRescueCompat;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbook;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientConfirmChargecastCatalystbookIndexPacket(InteractionHand hand, int selectedIndex) {
    public static void encode(ClientConfirmChargecastCatalystbookIndexPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeVarInt(packet.selectedIndex);
    }

    public static ClientConfirmChargecastCatalystbookIndexPacket decode(FriendlyByteBuf buffer) {
        return new ClientConfirmChargecastCatalystbookIndexPacket(
                buffer.readEnum(InteractionHand.class), buffer.readVarInt()
        );
    }

    public static void handle(ClientConfirmChargecastCatalystbookIndexPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || player.isSpectator() || MagicData.getPlayerMagicData(player).isCasting()) {
                return;
            }
            var stack = resolveHeldStack(player, packet.hand);
            if (!(stack.getItem() instanceof ChargecastCatalystbook)
                    || !ChargecastCatalystbook.isSelectableScrollIndex(stack, packet.selectedIndex)) {
                return;
            }
            ChargecastCatalystbook.setSelectedScrollIndex(stack, packet.selectedIndex);
        });
        context.setPacketHandled(true);
    }

    private static ItemStack resolveHeldStack(Player player, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND && ModList.get().isLoaded("bettercombat")) {
            return BetterCombatOffhandAttributeRescueCompat.getPhysicalOffhandStack(player);
        }
        return player.getItemInHand(hand);
    }
}
