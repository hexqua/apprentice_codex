package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowClientCastState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SyncElementalBowCastPacket(UUID playerId, String spellId, boolean active) implements CustomPacketPayload {
    public static final Type<SyncElementalBowCastPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_elemental_bow_cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncElementalBowCastPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeUUID(packet.playerId);
                buffer.writeUtf(packet.spellId);
                buffer.writeBoolean(packet.active);
            }, buffer -> new SyncElementalBowCastPacket(buffer.readUUID(), buffer.readUtf(), buffer.readBoolean()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncElementalBowCastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncElementalBowCastPacket packet) {
            ElementalBowClientCastState.sync(packet.playerId, packet.spellId, packet.active);
        }
    }
}
