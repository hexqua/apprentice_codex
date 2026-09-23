package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.ShootingStarMantleClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncMantlePacket(int entityId, boolean equipped, int energy, boolean recovering,
                              boolean hovering, boolean blink, long sequence, boolean accepted,
                              long blinkStart, long blinkSequence, double blinkHeight, Vec3 blinkDirection) implements CustomPacketPayload {
    public static final Type<SyncMantlePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mantle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMantlePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVarInt(packet.entityId); buffer.writeBoolean(packet.equipped); buffer.writeVarInt(packet.energy);
                buffer.writeBoolean(packet.recovering); buffer.writeBoolean(packet.hovering); buffer.writeBoolean(packet.blink);
                buffer.writeLong(packet.sequence); buffer.writeBoolean(packet.accepted);
                buffer.writeLong(packet.blinkStart); buffer.writeLong(packet.blinkSequence); buffer.writeDouble(packet.blinkHeight);
                buffer.writeVec3(packet.blinkDirection);
            }, buffer -> new SyncMantlePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readLong(), buffer.readBoolean(),
                    buffer.readLong(), buffer.readLong(), buffer.readDouble(), buffer.readVec3()));

    @Override
    public @NotNull Type<SyncMantlePacket> type() { return TYPE; }

    public static void handle(SyncMantlePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncMantlePacket packet) { ShootingStarMantleClient.accept(packet); }
    }
}
