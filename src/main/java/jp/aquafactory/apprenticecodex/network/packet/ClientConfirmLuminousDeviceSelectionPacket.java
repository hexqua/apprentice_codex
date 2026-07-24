package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientConfirmLuminousDeviceSelectionPacket(
        InteractionHand hand,
        LuminousDevice.Mode mode,
        ItemStack selectedStack
) {
    public ClientConfirmLuminousDeviceSelectionPacket {
        selectedStack = selectedStack.copyWithCount(1);
    }

    public static void encode(ClientConfirmLuminousDeviceSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand());
        buffer.writeEnum(packet.mode());
        buffer.writeItem(packet.selectedStack());
    }

    public static ClientConfirmLuminousDeviceSelectionPacket decode(FriendlyByteBuf buffer) {
        return new ClientConfirmLuminousDeviceSelectionPacket(
                buffer.readEnum(InteractionHand.class),
                buffer.readEnum(LuminousDevice.Mode.class),
                buffer.readItem()
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
                if (packet.mode() == LuminousDevice.Mode.CLEAN) {
                    LuminousDevice.setCleanMode(deviceStack);
                } else {
                    LuminousDevice.setSelectedStack(deviceStack, packet.selectedStack());
                }
            }
        });
        context.setPacketHandled(true);
    }
}
