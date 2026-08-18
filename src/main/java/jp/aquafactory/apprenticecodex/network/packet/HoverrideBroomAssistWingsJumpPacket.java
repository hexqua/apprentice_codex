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

public record HoverrideBroomAssistWingsJumpPacket(int entityId, float jumpHeight)
        implements CustomPacketPayload {
    private static final float MAX_JUMP_HEIGHT = 4.0F;
    public static final Type<HoverrideBroomAssistWingsJumpPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "hoverride_broom_assist_wings_jump")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, HoverrideBroomAssistWingsJumpPacket> STREAM_CODEC =
            StreamCodec.of(HoverrideBroomAssistWingsJumpPacket::encode, HoverrideBroomAssistWingsJumpPacket::decode);

    public HoverrideBroomAssistWingsJumpPacket {
        jumpHeight = Float.isFinite(jumpHeight)
                ? Math.clamp(jumpHeight, 0.0F, MAX_JUMP_HEIGHT)
                : 0.0F;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(FriendlyByteBuf buffer, HoverrideBroomAssistWingsJumpPacket packet) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeFloat(packet.jumpHeight);
    }

    public static HoverrideBroomAssistWingsJumpPacket decode(FriendlyByteBuf buffer) {
        return new HoverrideBroomAssistWingsJumpPacket(buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(HoverrideBroomAssistWingsJumpPacket packet, IPayloadContext context) {
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
