package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.item.magicitem.StorageStabilizer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientConfirmStorageStabilizerSpellPacket(
        InteractionHand hand,
        int selectedIndex
) {
    public static void encode(ClientConfirmStorageStabilizerSpellPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand());
        buffer.writeVarInt(packet.selectedIndex());
    }

    public static ClientConfirmStorageStabilizerSpellPacket decode(FriendlyByteBuf buffer) {
        return new ClientConfirmStorageStabilizerSpellPacket(
                buffer.readEnum(InteractionHand.class),
                buffer.readVarInt()
        );
    }

    public static void handle(ClientConfirmStorageStabilizerSpellPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

            var stack = sender.getItemInHand(packet.hand());
            if (!(stack.getItem() instanceof StorageStabilizer)
                    || !StorageStabilizer.isSelectableSpellIndex(packet.selectedIndex())) {
                return;
            }

            var previousIndex = StorageStabilizer.getSelectedSpellIndex(stack);
            StorageStabilizer.setSelectedSpellIndex(stack, packet.selectedIndex());
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
}
