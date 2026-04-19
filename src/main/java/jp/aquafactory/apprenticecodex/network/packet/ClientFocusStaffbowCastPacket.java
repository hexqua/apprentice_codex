package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.FocusStaffbowCastManager;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientFocusStaffbowCastPacket(
        int quickCastSlot,
        ResourceLocation spellId,
        BlockTargetData targetData
) {
    public ClientFocusStaffbowCastPacket(int quickCastSlot, ResourceLocation spellId, BlockTargetData targetData) {
        this.quickCastSlot = quickCastSlot;
        this.spellId = spellId;
        this.targetData = targetData == null ? new BlockTargetData() : targetData;
    }

    public static void encode(ClientFocusStaffbowCastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.quickCastSlot());
        buffer.writeResourceLocation(packet.spellId());
        packet.targetData().writeToBuffer(buffer);
    }

    public static ClientFocusStaffbowCastPacket decode(FriendlyByteBuf buffer) {
        var quickCastSlot = buffer.readInt();
        var spellId = buffer.readResourceLocation();
        var targetData = new BlockTargetData();
        targetData.readFromBuffer(buffer);
        return new ClientFocusStaffbowCastPacket(quickCastSlot, spellId, targetData);
    }

    public static void handle(ClientFocusStaffbowCastPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            FocusStaffbowCastManager.handleClientPacketInput(sender, packet.quickCastSlot(), packet.spellId(), packet.targetData());
        });
        context.setPacketHandled(true);
    }
}
