package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HoverrideBroomReleaseResultPacket(
        int entityId,
        long sequence,
        boolean accepted,
        double minimumHorizontalSpeed
) {

    public HoverrideBroomReleaseResultPacket {
        minimumHorizontalSpeed = Double.isFinite(minimumHorizontalSpeed)
                ? Math.max(0.0D, minimumHorizontalSpeed)
                : 0.0D;
    }

    public static void encode(HoverrideBroomReleaseResultPacket packet, FriendlyByteBuf buffer) {
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

    public static void handle(HoverrideBroomReleaseResultPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
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
