package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.swingstaff.SoulstainedSteelSwingcastStaffConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncSoulstainedSteelSwingcastStaffConfigPacket {
    private final double manaCostPerBlade;

    public SyncSoulstainedSteelSwingcastStaffConfigPacket(double manaCostPerBlade) {
        this.manaCostPerBlade = manaCostPerBlade;
    }

    public double manaCostPerBlade() {
        return manaCostPerBlade;
    }

    public static void encode(SyncSoulstainedSteelSwingcastStaffConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.manaCostPerBlade);
    }

    public static SyncSoulstainedSteelSwingcastStaffConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncSoulstainedSteelSwingcastStaffConfigPacket(buffer.readDouble());
    }

    public static void handle(
            SyncSoulstainedSteelSwingcastStaffConfigPacket packet,
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

        private static void handle(SyncSoulstainedSteelSwingcastStaffConfigPacket packet) {
            SoulstainedSteelSwingcastStaffConfigState.setManaCostPerBlade(packet.manaCostPerBlade);
        }
    }
}
