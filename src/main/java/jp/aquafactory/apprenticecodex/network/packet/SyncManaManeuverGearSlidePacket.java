package jp.aquafactory.apprenticecodex.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import net.minecraft.world.entity.LivingEntity;

public record SyncManaManeuverGearSlidePacket(double ySpeed) {


    public static void encode(SyncManaManeuverGearSlidePacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.ySpeed);
    }

    public static SyncManaManeuverGearSlidePacket decode(FriendlyByteBuf buffer) {
        return new SyncManaManeuverGearSlidePacket(buffer.readDouble());
    }

    public static void handle(SyncManaManeuverGearSlidePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> ClientHandler.handle(packet)));
        context.setPacketHandled(true);
    }

    public static void applyTo(LivingEntity entity, double ySpeed) {
        var currentVelocity = entity.getDeltaMovement();
        entity.setDeltaMovement(currentVelocity.x, ySpeed, currentVelocity.z);
        entity.hasImpulse = true;
        entity.fallDistance = 0.0F;
    }
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncManaManeuverGearSlidePacket packet) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                applyTo(player, packet.ySpeed);
            }
        }
    }
}
