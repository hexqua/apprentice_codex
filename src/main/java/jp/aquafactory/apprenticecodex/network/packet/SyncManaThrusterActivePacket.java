package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.event.client.ManaThrusterClientInputEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncManaThrusterActivePacket(boolean active) {
    public static void encode(SyncManaThrusterActivePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
    }

    public static SyncManaThrusterActivePacket decode(FriendlyByteBuf buffer) {
        return new SyncManaThrusterActivePacket(buffer.readBoolean());
    }

    public static void handle(SyncManaThrusterActivePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncManaThrusterActivePacket packet) {
            if (!packet.active) {
                ManaThrusterClientInputEvent.deactivateFromServer();
            }
        }
    }
}
