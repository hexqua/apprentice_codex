package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.smashcastscepter.SmashcastScepterClientRenderState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncSmashcastScepterReadyStatePacket {
    private final boolean ready;

    public SyncSmashcastScepterReadyStatePacket(boolean ready) {
        this.ready = ready;
    }

    public static void encode(SyncSmashcastScepterReadyStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.ready);
    }

    public static SyncSmashcastScepterReadyStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncSmashcastScepterReadyStatePacket(buffer.readBoolean());
    }

    public static void handle(SyncSmashcastScepterReadyStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncSmashcastScepterReadyStatePacket packet) {
            SmashcastScepterClientRenderState.setSyncedReadyState(packet.ready);
        }
    }
}
