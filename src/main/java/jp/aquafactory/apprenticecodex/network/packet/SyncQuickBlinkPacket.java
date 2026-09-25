package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.quickblink.QuickBlinkClient;
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

public record SyncQuickBlinkPacket(int entityId, long start, long sequence, Vec3 direction)
        implements CustomPacketPayload {
    public static final Type<SyncQuickBlinkPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_quick_blink"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncQuickBlinkPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVarInt(packet.entityId); buffer.writeLong(packet.start); buffer.writeLong(packet.sequence);
                buffer.writeVec3(packet.direction);
            }, buffer -> new SyncQuickBlinkPacket(buffer.readVarInt(), buffer.readLong(), buffer.readLong(), buffer.readVec3()));

    @Override
    public @NotNull Type<SyncQuickBlinkPacket> type() { return TYPE; }

    public static void handle(SyncQuickBlinkPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncQuickBlinkPacket packet) { QuickBlinkClient.accept(packet); }
    }
}
