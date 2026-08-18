package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record HoverrideBroomAssistWingsJumpPacket(int entityId, float jumpHeight) {
    private static final float MAX_JUMP_HEIGHT = 4.0F;

    public HoverrideBroomAssistWingsJumpPacket {
        jumpHeight = Float.isFinite(jumpHeight)
                ? Mth.clamp(jumpHeight, 0.0F, MAX_JUMP_HEIGHT)
                : 0.0F;
    }

    public static void encode(HoverrideBroomAssistWingsJumpPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeFloat(packet.jumpHeight);
    }

    public static HoverrideBroomAssistWingsJumpPacket decode(FriendlyByteBuf buffer) {
        return new HoverrideBroomAssistWingsJumpPacket(buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(HoverrideBroomAssistWingsJumpPacket packet,
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

        private static void handle(HoverrideBroomAssistWingsJumpPacket packet) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.player == null) {
                return;
            }
            if (minecraft.level.getEntity(packet.entityId) instanceof HoverrideBroomEntity broom
                    && minecraft.player.getVehicle() == broom
                    && broom.getControllingPassenger() == minecraft.player) {
                broom.acceptLocalAssistWingsJump(packet.jumpHeight);
            }
        }
    }
}
