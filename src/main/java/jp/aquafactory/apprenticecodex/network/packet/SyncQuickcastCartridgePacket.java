package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.QuickcastCartridgeClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncQuickcastCartridgePacket(boolean equipped, boolean available, boolean reserved, long serverTime,
        long recoveryUntil, long recoveryDuration, long reloadUntil, long reloadDuration, boolean completed)
        implements CustomPacketPayload {
    public static final Type<SyncQuickcastCartridgePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_quickcast_cartridge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncQuickcastCartridgePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeBoolean(packet.equipped);
                buffer.writeBoolean(packet.available);
                buffer.writeBoolean(packet.reserved);
                buffer.writeLong(packet.serverTime);
                buffer.writeLong(packet.recoveryUntil);
                buffer.writeLong(packet.recoveryDuration);
                buffer.writeLong(packet.reloadUntil);
                buffer.writeLong(packet.reloadDuration);
                buffer.writeBoolean(packet.completed);
            }, buffer -> new SyncQuickcastCartridgePacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readLong(), buffer.readLong(), buffer.readLong(), buffer.readLong(), buffer.readLong(), buffer.readBoolean()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncQuickcastCartridgePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        static void handle(SyncQuickcastCartridgePacket packet) { QuickcastCartridgeClientState.accept(packet); }
    }
}
