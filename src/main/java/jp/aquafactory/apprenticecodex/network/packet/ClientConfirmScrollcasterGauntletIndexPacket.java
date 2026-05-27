package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat;
import jp.aquafactory.apprenticecodex.item.ScrollcasterGauntlet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientConfirmScrollcasterGauntletIndexPacket(
        InteractionHand hand,
        int selectedIndex
) {
    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";

    public static void encode(ClientConfirmScrollcasterGauntletIndexPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand());
        buffer.writeVarInt(packet.selectedIndex());
    }

    public static ClientConfirmScrollcasterGauntletIndexPacket decode(FriendlyByteBuf buffer) {
        return new ClientConfirmScrollcasterGauntletIndexPacket(
                buffer.readEnum(InteractionHand.class),
                buffer.readVarInt()
        );
    }

    public static void handle(ClientConfirmScrollcasterGauntletIndexPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            var magicData = MagicData.getPlayerMagicData(sender);
            if (magicData != null && magicData.isCasting()) {
                return;
            }

            var stack = resolveHeldGauntletStack(sender, packet.hand());
            if (!(stack.getItem() instanceof ScrollcasterGauntlet)
                    || !ScrollcasterGauntlet.isSelectableScrollIndex(stack, packet.selectedIndex())) {
                return;
            }

            var previousIndex = ScrollcasterGauntlet.getSelectedScrollIndex(stack);
            ScrollcasterGauntlet.setSelectedScrollIndex(stack, packet.selectedIndex());
            if (previousIndex != packet.selectedIndex()) {
                sender.level().playSound(
                        null,
                        sender.blockPosition(),
                        SoundEvents.UI_BUTTON_CLICK.value(),
                        SoundSource.PLAYERS,
                        0.35F,
                        1.1F
                );
            }
        });
        context.setPacketHandled(true);
    }

    private static ItemStack resolveHeldGauntletStack(Player player, InteractionHand hand) {
        if (ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            return BetterCombatScrollcasterGauntletCompat.getResolvedHeldStack(player, hand);
        }
        return player.getItemInHand(hand);
    }
}
