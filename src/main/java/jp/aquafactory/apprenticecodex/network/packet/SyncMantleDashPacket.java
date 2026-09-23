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

public record SyncMantleDashPacket(int entityId, int kind, int level, boolean hover, long end, long revision, Vec3 motion) implements CustomPacketPayload {
    public static final Type<SyncMantleDashPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mantle_dash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMantleDashPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVarInt(packet.entityId); buffer.writeVarInt(packet.kind); buffer.writeVarInt(packet.level);
                buffer.writeBoolean(packet.hover); buffer.writeLong(packet.end); buffer.writeLong(packet.revision); buffer.writeVec3(packet.motion);
            }, buffer -> new SyncMantleDashPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readLong(), buffer.readLong(), buffer.readVec3()));

    @Override
    public @NotNull Type<SyncMantleDashPacket> type() { return TYPE; }

    public static void handle(SyncMantleDashPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> { if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet); });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncMantleDashPacket packet) { ShootingStarMantleClient.acceptDash(packet); }
    }
}
