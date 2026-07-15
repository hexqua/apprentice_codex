package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.bettercombat.BetterCombatScrollcasterGauntletCompat;
import jp.aquafactory.apprenticecodex.item.scrollcastergauntlet.ScrollcasterGauntlet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientConfirmScrollcasterGauntletIndexPacket(
        InteractionHand hand,
        int selectedIndex
) implements CustomPacketPayload {
    private static final String BETTER_COMBAT_MOD_ID = "bettercombat";
    public static final Type<ClientConfirmScrollcasterGauntletIndexPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_confirm_scrollcaster_gauntlet_index"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientConfirmScrollcasterGauntletIndexPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientConfirmScrollcasterGauntletIndexPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(ClientConfirmScrollcasterGauntletIndexPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            var magicData = MagicData.getPlayerMagicData(sender);
            if (magicData != null && magicData.isCasting()) {
                return;
            }

            var stack = resolveHeldGauntletStack(sender, packet.hand());
            var lookupProvider = sender.level().registryAccess();
            if (!(stack.getItem() instanceof ScrollcasterGauntlet)
                    || !ScrollcasterGauntlet.isSelectableScrollIndex(
                            stack,
                            packet.selectedIndex(),
                            lookupProvider
                    )) {
                return;
            }

            var previousIndex = ScrollcasterGauntlet.getSelectedScrollIndex(stack);
            ScrollcasterGauntlet.setSelectedScrollIndex(stack, packet.selectedIndex(), lookupProvider);
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
    }

    private static ItemStack resolveHeldGauntletStack(Player player, InteractionHand hand) {
        if (ModList.get().isLoaded(BETTER_COMBAT_MOD_ID)) {
            return BetterCombatScrollcasterGauntletCompat.getResolvedHeldStack(player, hand);
        }
        return player.getItemInHand(hand);
    }
}
