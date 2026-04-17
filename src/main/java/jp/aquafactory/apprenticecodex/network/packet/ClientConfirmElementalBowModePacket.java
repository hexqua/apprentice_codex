package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.ElementalBow;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record ClientConfirmElementalBowModePacket(
        InteractionHand hand,
        String shotMode,
        @Nullable ResourceLocation selectionId,
        boolean continueUse
) {
    public static void encode(ClientConfirmElementalBowModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand());
        buffer.writeUtf(packet.shotMode());
        buffer.writeBoolean(packet.selectionId() != null);
        if (packet.selectionId() != null) {
            buffer.writeResourceLocation(packet.selectionId());
        }
        buffer.writeBoolean(packet.continueUse());
    }

    public static ClientConfirmElementalBowModePacket decode(FriendlyByteBuf buffer) {
        var hand = buffer.readEnum(InteractionHand.class);
        var shotMode = buffer.readUtf();
        var hasSelectionId = buffer.readBoolean();
        var selectionId = hasSelectionId ? buffer.readResourceLocation() : null;
        var continueUse = buffer.readBoolean();
        return new ClientConfirmElementalBowModePacket(hand, shotMode, selectionId, continueUse);
    }

    public static void handle(ClientConfirmElementalBowModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            ElementalBow.applyClientSelection(sender, packet.hand(), packet.shotMode(), packet.selectionId(), packet.continueUse());
        });
        context.setPacketHandled(true);
    }
}
