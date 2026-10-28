package jp.aquafactory.apprenticecodex.network.packet;

import java.util.function.Supplier;
import jp.aquafactory.apprenticecodex.config.item.ManaForceReleaseDeviceServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.manaforcereleasedevice.ManaForceReleaseClientConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

public final class SyncManaForceReleaseDeviceConfigPacket {
    private final Values values;

    public SyncManaForceReleaseDeviceConfigPacket(Values values) {
        this.values = values;
    }

    public static void encode(SyncManaForceReleaseDeviceConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.values.noviceMultiplier());
        buffer.writeDouble(packet.values.adeptMultiplier());
        buffer.writeDouble(packet.values.masterMultiplier());
        buffer.writeDouble(packet.values.noviceSoftCap());
        buffer.writeDouble(packet.values.adeptSoftCap());
    }

    public static SyncManaForceReleaseDeviceConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaForceReleaseDeviceConfigPacket(new Values(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(SyncManaForceReleaseDeviceConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncManaForceReleaseDeviceConfigPacket packet) {
            ManaForceReleaseClientConfig.set(packet.values);
        }
    }
}
