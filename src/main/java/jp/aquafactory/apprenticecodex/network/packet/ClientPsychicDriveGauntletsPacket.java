package jp.aquafactory.apprenticecodex.network.packet;

import java.util.function.Supplier;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackEvents;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.Input;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ClientPsychicDriveGauntletsPacket(Input input) {
    public static void encode(ClientPsychicDriveGauntletsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.input);
    }

    public static ClientPsychicDriveGauntletsPacket decode(FriendlyByteBuf buffer) {
        return new ClientPsychicDriveGauntletsPacket(buffer.readEnum(Input.class));
    }

    public static void handle(ClientPsychicDriveGauntletsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var player = context.getSender();
            if (player != null) PsychicDriveGauntletsAttackEvents.input(player, packet.input);
        });
        context.setPacketHandled(true);
    }
}
