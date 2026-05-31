package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaffConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncZenithStaffConfigPacket {
    private final float manaCostMultiplier;

    public SyncZenithStaffConfigPacket(float manaCostMultiplier) {
        this.manaCostMultiplier = manaCostMultiplier;
    }

    public static void encode(SyncZenithStaffConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.manaCostMultiplier);
    }

    public static SyncZenithStaffConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncZenithStaffConfigPacket(buffer.readFloat());
    }

    public static void handle(SyncZenithStaffConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncZenithStaffConfigPacket packet) {
            ZenithStaffConfigState.setManaCostMultiplier(packet.manaCostMultiplier);
        }
    }
}
