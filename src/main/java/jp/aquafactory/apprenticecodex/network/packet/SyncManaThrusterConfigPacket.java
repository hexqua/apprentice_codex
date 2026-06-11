package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncManaThrusterConfigPacket {
    private final float manaCostPerTick;

    public SyncManaThrusterConfigPacket(float manaCostPerTick) {
        this.manaCostPerTick = manaCostPerTick;
    }

    public static void encode(SyncManaThrusterConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.manaCostPerTick);
    }

    public static SyncManaThrusterConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaThrusterConfigPacket(buffer.readFloat());
    }

    public static void handle(SyncManaThrusterConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncManaThrusterConfigPacket packet) {
            ManaThrusterConfigState.set(packet.manaCostPerTick);
        }
    }
}
