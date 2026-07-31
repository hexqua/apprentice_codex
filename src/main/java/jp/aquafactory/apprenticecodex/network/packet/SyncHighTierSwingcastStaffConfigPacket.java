package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.swingstaff.HighTierSwingcastStaffConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncHighTierSwingcastStaffConfigPacket {
    private final int diamondCooldownReductionTicks;
    private final int netheriteCooldownReductionTicks;

    public SyncHighTierSwingcastStaffConfigPacket(
            int diamondCooldownReductionTicks,
            int netheriteCooldownReductionTicks
    ) {
        this.diamondCooldownReductionTicks = diamondCooldownReductionTicks;
        this.netheriteCooldownReductionTicks = netheriteCooldownReductionTicks;
    }

    public static void encode(SyncHighTierSwingcastStaffConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.diamondCooldownReductionTicks);
        buffer.writeVarInt(packet.netheriteCooldownReductionTicks);
    }

    public static SyncHighTierSwingcastStaffConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncHighTierSwingcastStaffConfigPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(
            SyncHighTierSwingcastStaffConfigPacket packet,
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

        private static void handle(SyncHighTierSwingcastStaffConfigPacket packet) {
            HighTierSwingcastStaffConfigState.setCooldownReductionTicks(
                    packet.diamondCooldownReductionTicks,
                    packet.netheriteCooldownReductionTicks
            );
        }
    }
}
