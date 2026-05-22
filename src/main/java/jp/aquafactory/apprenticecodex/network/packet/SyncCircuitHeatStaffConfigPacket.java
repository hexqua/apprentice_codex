package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncCircuitHeatStaffConfigPacket {
    private final int cooldownBypassMaxRemainingTicks;

    public SyncCircuitHeatStaffConfigPacket(int cooldownBypassMaxRemainingTicks) {
        this.cooldownBypassMaxRemainingTicks = cooldownBypassMaxRemainingTicks;
    }

    public static void encode(SyncCircuitHeatStaffConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.cooldownBypassMaxRemainingTicks);
    }

    public static SyncCircuitHeatStaffConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncCircuitHeatStaffConfigPacket(buffer.readVarInt());
    }

    public static void handle(SyncCircuitHeatStaffConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncCircuitHeatStaffConfigPacket packet) {
            CircuitHeatStaffConfigState.setCooldownBypassMaxRemainingTicks(packet.cooldownBypassMaxRemainingTicks);
        }
    }
}
