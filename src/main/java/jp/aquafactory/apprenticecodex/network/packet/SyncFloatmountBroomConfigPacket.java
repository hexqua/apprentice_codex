package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.FloatmountBroomConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncFloatmountBroomConfigPacket(int normalFlightManaThreshold) {
    public static void encode(SyncFloatmountBroomConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.normalFlightManaThreshold);
    }

    public static SyncFloatmountBroomConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncFloatmountBroomConfigPacket(buffer.readVarInt());
    }

    public static void handle(SyncFloatmountBroomConfigPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncFloatmountBroomConfigPacket packet) {
            FloatmountBroomConfigState.setNormalFlightManaThreshold(packet.normalFlightManaThreshold);
        }
    }
}
