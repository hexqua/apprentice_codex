package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record ClientConfirmLuminousDeviceSelectionPacket(
        InteractionHand hand,
        LuminousDevice.Mode mode,
        ItemStack selectedStack,
        @Nullable ResourceLocation selectedSpellId
) {
    public ClientConfirmLuminousDeviceSelectionPacket {
        selectedStack = selectedStack.copyWithCount(1);
    }

    public static void encode(ClientConfirmLuminousDeviceSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand());
        buffer.writeEnum(packet.mode());
        buffer.writeItem(packet.selectedStack());
        buffer.writeBoolean(packet.selectedSpellId() != null);
        if (packet.selectedSpellId() != null) {
            buffer.writeResourceLocation(packet.selectedSpellId());
        }
    }

    public static ClientConfirmLuminousDeviceSelectionPacket decode(FriendlyByteBuf buffer) {
        return new ClientConfirmLuminousDeviceSelectionPacket(
                buffer.readEnum(InteractionHand.class),
                buffer.readEnum(LuminousDevice.Mode.class),
                buffer.readItem(),
                buffer.readBoolean() ? buffer.readResourceLocation() : null
        );
    }

    public static void handle(
            ClientConfirmLuminousDeviceSelectionPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            var deviceStack = sender.getItemInHand(packet.hand());
            if (deviceStack.getItem() instanceof LuminousDevice) {
                switch (packet.mode()) {
                    case CLEAN -> LuminousDevice.setCleanMode(deviceStack);
                    case SPELL -> LuminousDevice.setSelectedSpell(deviceStack, packet.selectedSpellId());
                    case PLACE -> LuminousDevice.setSelectedStack(deviceStack, packet.selectedStack());
                }
            }
        });
        context.setPacketHandled(true);
    }
}
