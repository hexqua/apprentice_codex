package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.item.armor.ElementMaidenRobeSchoolPowerBonusEvents;
import jp.aquafactory.apprenticecodex.item.curios.archivistsgrimoire.ArchivistsGrimoire;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientChangeArchivistsGrimoireRowPacket {
    private final int delta;

    public ClientChangeArchivistsGrimoireRowPacket(int delta) {
        this.delta = Integer.compare(delta, 0);
    }

    public static void encode(ClientChangeArchivistsGrimoireRowPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.delta);
    }

    public static ClientChangeArchivistsGrimoireRowPacket decode(FriendlyByteBuf buffer) {
        return new ClientChangeArchivistsGrimoireRowPacket(buffer.readByte());
    }

    public static void handle(ClientChangeArchivistsGrimoireRowPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || packet.delta == 0) {
                return;
            }

            var spellbookStack = Utils.getPlayerSpellbookStack(sender);
            if (spellbookStack != null && spellbookStack.getItem() instanceof ArchivistsGrimoire) {
                if (ArchivistsGrimoire.changeSelectedRowToPopulatedRow(spellbookStack, packet.delta)) {
                    ElementMaidenRobeSchoolPowerBonusEvents.refresh(sender);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
