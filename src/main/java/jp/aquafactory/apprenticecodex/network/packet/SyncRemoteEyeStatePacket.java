package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncRemoteEyeStatePacket {
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

    public static void handle(SyncRemoteEyeStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
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
            player.getCapability(Capabilities.SPELL_DATA).ifPresent(data ->
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
