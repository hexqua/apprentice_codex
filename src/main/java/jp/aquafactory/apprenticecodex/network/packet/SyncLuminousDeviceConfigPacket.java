package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.luminousdevice.LuminousDeviceConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncLuminousDeviceConfigPacket(int maxStoredItems, int maxStoredMana) {
    public static void encode(SyncLuminousDeviceConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.maxStoredItems);
        buffer.writeVarInt(packet.maxStoredMana);
    }

    public static SyncLuminousDeviceConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncLuminousDeviceConfigPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(
            SyncLuminousDeviceConfigPacket packet,
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

        private static void handle(SyncLuminousDeviceConfigPacket packet) {
            LuminousDeviceConfigState.set(packet.maxStoredItems, packet.maxStoredMana);
        }
    }
}
