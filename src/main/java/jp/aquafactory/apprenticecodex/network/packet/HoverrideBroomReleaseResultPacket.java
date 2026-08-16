package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
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
import org.jetbrains.annotations.NotNull;

public record HoverrideBroomReleaseResultPacket(
        int entityId,
        long sequence,
        boolean accepted,
        double minimumHorizontalSpeed
) implements CustomPacketPayload {
    public static final Type<HoverrideBroomReleaseResultPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "hoverride_broom_release_result")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, HoverrideBroomReleaseResultPacket> STREAM_CODEC =
            StreamCodec.of(HoverrideBroomReleaseResultPacket::encode, HoverrideBroomReleaseResultPacket::decode);

    public HoverrideBroomReleaseResultPacket {
        minimumHorizontalSpeed = Double.isFinite(minimumHorizontalSpeed)
                ? Math.max(0.0D, minimumHorizontalSpeed)
                : 0.0D;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(FriendlyByteBuf buffer, HoverrideBroomReleaseResultPacket packet) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarLong(packet.sequence);
        buffer.writeBoolean(packet.accepted);
        buffer.writeDouble(packet.minimumHorizontalSpeed);
    }

    public static HoverrideBroomReleaseResultPacket decode(FriendlyByteBuf buffer) {
        return new HoverrideBroomReleaseResultPacket(
                buffer.readVarInt(),
                buffer.readVarLong(),
                buffer.readBoolean(),
                buffer.readDouble()
        );
    }

    public static void handle(HoverrideBroomReleaseResultPacket packet, IPayloadContext context) {
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

        private static void handle(HoverrideBroomReleaseResultPacket packet) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                return;
            }
            if (minecraft.level.getEntity(packet.entityId) instanceof HoverrideBroomEntity broom
                    && minecraft.player.getVehicle() == broom
                    && broom.getControllingPassenger() == minecraft.player) {
                broom.acceptLocalReleaseResult(
                        packet.sequence,
                        packet.accepted,
                        packet.minimumHorizontalSpeed
                );
            }
        }
    }
}
