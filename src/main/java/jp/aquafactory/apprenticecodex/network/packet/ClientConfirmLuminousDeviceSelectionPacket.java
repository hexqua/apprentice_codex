package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDevice;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public record ClientConfirmLuminousDeviceSelectionPacket(
        InteractionHand hand,
        LuminousDevice.Mode mode,
        ItemStack selectedStack,
        @Nullable ResourceLocation selectedSpellId
) implements CustomPacketPayload {
    public static final Type<ClientConfirmLuminousDeviceSelectionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_confirm_luminous_device_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientConfirmLuminousDeviceSelectionPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientConfirmLuminousDeviceSelectionPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public ClientConfirmLuminousDeviceSelectionPacket {
        selectedStack = selectedStack.copyWithCount(1);
    }

    public static void encode(ClientConfirmLuminousDeviceSelectionPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand());
        buffer.writeEnum(packet.mode());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.selectedStack());
        buffer.writeBoolean(packet.selectedSpellId() != null);
        if (packet.selectedSpellId() != null) {
            buffer.writeResourceLocation(packet.selectedSpellId());
        }
    }

    public static ClientConfirmLuminousDeviceSelectionPacket decode(RegistryFriendlyByteBuf buffer) {
        return new ClientConfirmLuminousDeviceSelectionPacket(
                buffer.readEnum(InteractionHand.class),
                buffer.readEnum(LuminousDevice.Mode.class),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                buffer.readBoolean() ? buffer.readResourceLocation() : null
        );
    }

    public static void handle(ClientConfirmLuminousDeviceSelectionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var sender = context.player();
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
    }
}
