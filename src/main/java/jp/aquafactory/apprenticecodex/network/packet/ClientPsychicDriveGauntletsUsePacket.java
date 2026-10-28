package jp.aquafactory.apprenticecodex.network.packet;

import java.util.function.Supplier;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackEvents;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.UseAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

public record ClientPsychicDriveGauntletsUsePacket(UseAction action, InteractionHand hand, long session) {
    public static void encode(ClientPsychicDriveGauntletsUsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
        buffer.writeEnum(packet.hand);
        buffer.writeLong(packet.session);
    }

    public static ClientPsychicDriveGauntletsUsePacket decode(FriendlyByteBuf buffer) {
        return new ClientPsychicDriveGauntletsUsePacket(buffer.readEnum(UseAction.class),
                buffer.readEnum(InteractionHand.class), buffer.readLong());
    }

    public static void handle(ClientPsychicDriveGauntletsUsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) {
                PsychicDriveGauntletsAttackEvents.useInput(player, packet.action, packet.hand, packet.session);
            }
        });
        context.setPacketHandled(true);
    }
}
