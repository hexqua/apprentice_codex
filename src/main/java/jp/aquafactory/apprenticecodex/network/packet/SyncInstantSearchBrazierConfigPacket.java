package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.magicitem.client.InstantSearchBrazierConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncInstantSearchBrazierConfigPacket(int initialRange) {
    public static void encode(SyncInstantSearchBrazierConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.initialRange);
    }

    public static SyncInstantSearchBrazierConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncInstantSearchBrazierConfigPacket(buffer.readVarInt());
    }

    public static void handle(
            SyncInstantSearchBrazierConfigPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
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

        private static void handle(SyncInstantSearchBrazierConfigPacket packet) {
            InstantSearchBrazierConfigState.set(packet.initialRange);
        }
    }
}
