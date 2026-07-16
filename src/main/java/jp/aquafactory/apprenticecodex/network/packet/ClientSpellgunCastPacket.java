package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.spellgun.AbstractSpellGunItem;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientSpellgunCastPacket(BlockTargetData targetData) {
    public ClientSpellgunCastPacket {
        if (targetData == null) {
            targetData = new BlockTargetData();
        }
    }

    public static void encode(ClientSpellgunCastPacket packet, FriendlyByteBuf buffer) {
        packet.targetData().writeToBuffer(buffer);
    }

    public static ClientSpellgunCastPacket decode(FriendlyByteBuf buffer) {
        var targetData = new BlockTargetData();
        targetData.readFromBuffer(buffer);
        return new ClientSpellgunCastPacket(targetData);
    }

    public static void handle(ClientSpellgunCastPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            if (sender.getMainHandItem().getItem() instanceof AbstractSpellGunItem spellgun) {
                spellgun.tryTriggerImbuedSpell(sender, InteractionHand.MAIN_HAND, packet.targetData());
            }
        });
        context.setPacketHandled(true);
    }
}
