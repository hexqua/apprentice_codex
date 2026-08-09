package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomDismountEvents;
import jp.aquafactory.apprenticecodex.entity.floatmountbroom.FloatmountBroomEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientFloatmountBroomDismountInputPacket(int broomId, boolean pressed)
        implements CustomPacketPayload {
    public static final Type<ClientFloatmountBroomDismountInputPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_floatmount_broom_dismount_input")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientFloatmountBroomDismountInputPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeVarInt(packet.broomId);
                buffer.writeBoolean(packet.pressed);
            }, buffer -> new ClientFloatmountBroomDismountInputPacket(
                    buffer.readVarInt(),
                    buffer.readBoolean()
            ));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientFloatmountBroomDismountInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)
                    || !(sender.getVehicle() instanceof FloatmountBroomEntity broom)
                    || broom.getId() != packet.broomId
                    || broom.getControllingPassenger() != sender) {
                return;
            }
            FloatmountBroomDismountEvents.handleSneakInput(sender, broom, packet.pressed);
        });
    }
}
