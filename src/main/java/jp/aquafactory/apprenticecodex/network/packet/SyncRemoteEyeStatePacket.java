package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SyncRemoteEyeStatePacket implements CustomPacketPayload {
    public static final Type<SyncRemoteEyeStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_remote_eye_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRemoteEyeStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncRemoteEyeStatePacket::decode);

    private final long activeUntilGameTime;
    private final double anchorX;
    private final double anchorY;
    private final double anchorZ;
    private final float anchorYaw;
    private final float anchorPitch;

    public SyncRemoteEyeStatePacket(long activeUntilGameTime, double anchorX, double anchorY, double anchorZ, float anchorYaw, float anchorPitch) {
        this.activeUntilGameTime = activeUntilGameTime;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.anchorYaw = anchorYaw;
        this.anchorPitch = anchorPitch;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncRemoteEyeStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.activeUntilGameTime);
        buffer.writeDouble(packet.anchorX);
        buffer.writeDouble(packet.anchorY);
        buffer.writeDouble(packet.anchorZ);
        buffer.writeFloat(packet.anchorYaw);
        buffer.writeFloat(packet.anchorPitch);
    }

    public static SyncRemoteEyeStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncRemoteEyeStatePacket(
                buffer.readLong(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(SyncRemoteEyeStatePacket packet, IPayloadContext context) {
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

        private static void handle(SyncRemoteEyeStatePacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            // 発動判定はサーバー起点なので、クライアント側の視点切替条件も明示同期する.
            Capabilities.getSpellData(player).ifPresent(data ->
                    data.edit(CodexSpellStateTypeRegister.REMOTE_EYE_STATE, state -> {
                        state.activeUntilGameTime = packet.activeUntilGameTime;
                        state.anchorX = packet.anchorX;
                        state.anchorY = packet.anchorY;
                        state.anchorZ = packet.anchorZ;
                        state.anchorYaw = packet.anchorYaw;
                        state.anchorPitch = packet.anchorPitch;
                    })
            );
        }
    }
}
