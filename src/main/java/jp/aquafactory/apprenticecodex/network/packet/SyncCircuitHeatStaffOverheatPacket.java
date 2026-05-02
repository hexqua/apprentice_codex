package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SyncCircuitHeatStaffOverheatPacket {
    private final CompoundTag data;

    public SyncCircuitHeatStaffOverheatPacket(@Nullable CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    public static void encode(SyncCircuitHeatStaffOverheatPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static SyncCircuitHeatStaffOverheatPacket decode(FriendlyByteBuf buffer) {
        return new SyncCircuitHeatStaffOverheatPacket(buffer.readNbt());
    }

    public static void handle(SyncCircuitHeatStaffOverheatPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncCircuitHeatStaffOverheatPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            CircuitHeatStaffOverheatManager.applySyncedState(player, packet.data);
        }
    }
}
