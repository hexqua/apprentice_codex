package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.config.item.ChargedTwinBladeStaffServerConfig;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffClientConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SyncChargedTwinBladeStaffConfigPacket(
        ChargedTwinBladeStaffServerConfig.Values values
) {

    public static void encode(SyncChargedTwinBladeStaffConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.values.riptideInitialManaCost());
        buffer.writeVarInt(packet.values.riptideSustainManaCostPer10Ticks());
    }

    public static SyncChargedTwinBladeStaffConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncChargedTwinBladeStaffConfigPacket(new ChargedTwinBladeStaffServerConfig.Values(
                buffer.readVarInt(), buffer.readVarInt()
        ));
    }

    public static void handle(SyncChargedTwinBladeStaffConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncChargedTwinBladeStaffConfigPacket packet) {
            ChargedTwinBladeStaffClientConfigState.set(packet.values);
        }
    }
}
