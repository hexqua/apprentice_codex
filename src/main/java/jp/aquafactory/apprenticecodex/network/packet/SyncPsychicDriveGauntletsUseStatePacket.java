package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.PsychicDriveGauntletsUseInput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncPsychicDriveGauntletsUseStatePacket(long session, boolean charging) implements CustomPacketPayload {
    public static final Type<SyncPsychicDriveGauntletsUseStatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_psychic_drive_gauntlets_use_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPsychicDriveGauntletsUseStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeLong(packet.session);
                buffer.writeBoolean(packet.charging);
            }, buffer -> new SyncPsychicDriveGauntletsUseStatePacket(buffer.readLong(), buffer.readBoolean()));

    @Override public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SyncPsychicDriveGauntletsUseStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.accept(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void accept(SyncPsychicDriveGauntletsUseStatePacket packet) {
            PsychicDriveGauntletsUseInput.accept(packet.session, packet.charging);
        }
    }
}
