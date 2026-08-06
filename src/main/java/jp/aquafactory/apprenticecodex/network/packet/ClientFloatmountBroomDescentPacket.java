package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientFloatmountBroomDescentPacket(boolean descending) implements CustomPacketPayload {
    public static final Type<ClientFloatmountBroomDescentPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_floatmount_broom_descent")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientFloatmountBroomDescentPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> buffer.writeBoolean(packet.descending),
                    buffer -> new ClientFloatmountBroomDescentPacket(buffer.readBoolean()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientFloatmountBroomDescentPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)
                    || !(sender.getVehicle() instanceof FloatmountBroomEntity broom)
                    || broom.getControllingPassenger() != sender) {
                return;
            }
            broom.setDescendingInput(packet.descending);
        });
    }
}
