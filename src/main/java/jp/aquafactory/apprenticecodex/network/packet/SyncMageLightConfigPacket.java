package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.magelight.MageLightConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncMageLightConfigPacket(double maxRange) {
    public static void encode(SyncMageLightConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.maxRange);
    }

    public static SyncMageLightConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncMageLightConfigPacket(buffer.readDouble());
    }

    public static void handle(SyncMageLightConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncMageLightConfigPacket packet) {
            MageLightConfigState.set(packet.maxRange);
        }
    }
}
