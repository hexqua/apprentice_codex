package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientFullautoRapidcastSpellrifleCastPacket(
        boolean adsFullAuto,
        BlockTargetData targetData
) {

    public static void encode(ClientFullautoRapidcastSpellrifleCastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.adsFullAuto());
        packet.targetData().writeToBuffer(buffer);
    }

    public static ClientFullautoRapidcastSpellrifleCastPacket decode(FriendlyByteBuf buffer) {
        var adsFullAuto = buffer.readBoolean();
        var targetData = new BlockTargetData();
        targetData.readFromBuffer(buffer);
        return new ClientFullautoRapidcastSpellrifleCastPacket(adsFullAuto, targetData);
    }

    public static void handle(ClientFullautoRapidcastSpellrifleCastPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            var mainHandItem = sender.getMainHandItem().getItem();
            if (mainHandItem instanceof FullautoRapidcastSpellrifle staffrifle) {
                var casted = staffrifle.tryTriggerSelectedSpell(sender, packet.adsFullAuto(), packet.targetData());
                if (casted && ModList.get().isLoaded(EpicFightSwingMagicCompat.MOD_ID)) {
                    EpicFightSwingMagicCompat.playStaffrifleShotAnimation(sender);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
