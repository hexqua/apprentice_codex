package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightCompat;
import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientSpellgunCastPacket(BlockTargetData targetData, boolean deferToEpicFightAttack) {
    public ClientSpellgunCastPacket(BlockTargetData targetData) {
        this(targetData, false);
    }

    public ClientSpellgunCastPacket {
        if (targetData == null) {
            targetData = new BlockTargetData();
        }
    }

    public static void encode(ClientSpellgunCastPacket packet, FriendlyByteBuf buffer) {
        packet.targetData().writeToBuffer(buffer);
        buffer.writeBoolean(packet.deferToEpicFightAttack());
    }

    public static ClientSpellgunCastPacket decode(FriendlyByteBuf buffer) {
        var targetData = new BlockTargetData();
        targetData.readFromBuffer(buffer);
        return new ClientSpellgunCastPacket(targetData, buffer.readBoolean());
    }

    public static void handle(ClientSpellgunCastPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            if (packet.deferToEpicFightAttack()
                    && EpicFightCompat.queueMainhandSpellgunCast(sender, packet.targetData())) {
                return;
            }

            if (sender.getMainHandItem().getItem() instanceof AbstractSpellGunItem spellgun) {
                spellgun.tryTriggerImbuedSpell(sender, InteractionHand.MAIN_HAND, packet.targetData());
            }
        });
        context.setPacketHandled(true);
    }
}
