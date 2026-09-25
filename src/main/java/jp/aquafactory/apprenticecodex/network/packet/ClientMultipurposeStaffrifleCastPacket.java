package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientMultipurposeStaffrifleCastPacket(boolean aiming, BlockTargetData targetData) {
    public static void encode(ClientMultipurposeStaffrifleCastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.aiming());
        packet.targetData().writeToBuffer(buffer);
    }

    public static ClientMultipurposeStaffrifleCastPacket decode(FriendlyByteBuf buffer) {
        var aiming = buffer.readBoolean();
        var targetData = new BlockTargetData();
        targetData.readFromBuffer(buffer);
        return new ClientMultipurposeStaffrifleCastPacket(aiming, targetData);
    }

    public static void handle(ClientMultipurposeStaffrifleCastPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            var mainHandItem = sender.getMainHandItem().getItem();
            if (mainHandItem instanceof MultipurposeStaffrifle staffrifle) {
                var casted = staffrifle.tryTriggerSelectedSpell(sender, packet.aiming(), packet.targetData());
                if (casted && ModList.get().isLoaded(EpicFightSwingMagicCompat.MOD_ID)) {
                    EpicFightSwingMagicCompat.playStaffrifleShotAnimation(sender);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
