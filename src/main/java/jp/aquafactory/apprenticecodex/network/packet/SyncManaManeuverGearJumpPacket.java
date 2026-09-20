package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.manamaneuvergear.ManaManeuverGearMovement;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.world.phys.Vec3;

public record SyncManaManeuverGearJumpPacket(Vec3 impulse) {


    public static void encode(SyncManaManeuverGearJumpPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.impulse.x);
        buffer.writeDouble(packet.impulse.y);
        buffer.writeDouble(packet.impulse.z);
    }

    public static SyncManaManeuverGearJumpPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaManeuverGearJumpPacket(new Vec3(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        ));
    }

    public static void handle(SyncManaManeuverGearJumpPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> ClientHandler.handle(packet)));
        context.setPacketHandled(true);
    }
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncManaManeuverGearJumpPacket packet) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                ManaManeuverGearMovement.applyWallJump(player, packet.impulse);
            }
        }
    }
}
