package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.config.item.ManaForceReleaseDeviceServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.manaforcereleasedevice.ManaForceReleaseClientConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public final class SyncManaForceReleaseDeviceConfigPacket implements CustomPacketPayload {
    public static final Type<SyncManaForceReleaseDeviceConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mana_force_release_device_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaForceReleaseDeviceConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncManaForceReleaseDeviceConfigPacket::decode);

    private final Values values;

    public SyncManaForceReleaseDeviceConfigPacket(Values values) {
        this.values = values;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncManaForceReleaseDeviceConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.values.noviceMultiplier());
        buffer.writeDouble(packet.values.adeptMultiplier());
        buffer.writeDouble(packet.values.masterMultiplier());
        buffer.writeDouble(packet.values.noviceSoftCap());
        buffer.writeDouble(packet.values.adeptSoftCap());
    }

    private static SyncManaForceReleaseDeviceConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaForceReleaseDeviceConfigPacket(new Values(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    public static void handle(SyncManaForceReleaseDeviceConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncManaForceReleaseDeviceConfigPacket packet) {
            ManaForceReleaseClientConfig.set(packet.values);
        }
    }
}
