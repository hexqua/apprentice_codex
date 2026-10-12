package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import jp.aquafactory.apprenticecodex.event.client.ManaStabilizerClientEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModList;

public record SyncManaStabilizerRecoilPacket(long tick, long revision, long shot, float pitch, float yaw, boolean paid) implements CustomPacketPayload {
    public static final Type<SyncManaStabilizerRecoilPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mana_stabilizer_recoil"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaStabilizerRecoilPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeLong(packet.tick()); buffer.writeVarLong(packet.revision()); buffer.writeVarLong(packet.shot()); buffer.writeFloat(packet.pitch()); buffer.writeFloat(packet.yaw()); buffer.writeBoolean(packet.paid()); },
            buffer -> new SyncManaStabilizerRecoilPacket(buffer.readLong(), buffer.readVarLong(), buffer.readVarLong(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean()));
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncManaStabilizerRecoilPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT && ModList.get().isLoaded("irons_artifice")) ManaStabilizerClientEvents.accept(packet);
        });
    }
}

