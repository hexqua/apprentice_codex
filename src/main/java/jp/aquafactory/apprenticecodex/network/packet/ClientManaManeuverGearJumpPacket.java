package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear.ManaManeuverGearManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

public record ClientManaManeuverGearJumpPacket() {


    public static void encode(ClientManaManeuverGearJumpPacket packet, FriendlyByteBuf buffer) {
    }

    public static ClientManaManeuverGearJumpPacket decode(FriendlyByteBuf buffer) {
        return new ClientManaManeuverGearJumpPacket();
    }

    public static void handle(ClientManaManeuverGearJumpPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender != null && !sender.isSpectator()) {
                ManaManeuverGearManager.tryWallJump(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
