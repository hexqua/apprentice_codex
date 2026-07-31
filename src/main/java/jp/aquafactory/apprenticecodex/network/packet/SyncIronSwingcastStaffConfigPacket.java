package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.swingstaff.IronSwingcastStaffConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncIronSwingcastStaffConfigPacket {
    private final double crystallineArcaneShardDropChance;

    public SyncIronSwingcastStaffConfigPacket(double crystallineArcaneShardDropChance) {
        this.crystallineArcaneShardDropChance = crystallineArcaneShardDropChance;
    }

    public static void encode(SyncIronSwingcastStaffConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.crystallineArcaneShardDropChance);
    }

    public static SyncIronSwingcastStaffConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncIronSwingcastStaffConfigPacket(buffer.readDouble());
    }

    public static void handle(
            SyncIronSwingcastStaffConfigPacket packet,
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

        private static void handle(SyncIronSwingcastStaffConfigPacket packet) {
            IronSwingcastStaffConfigState.setCrystallineArcaneShardDropChance(
                    packet.crystallineArcaneShardDropChance
            );
        }
    }
}