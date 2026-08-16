package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.BroomDismountEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientBroomDismountInputPacket(int broomId, boolean pressed)
        implements CustomPacketPayload {
    public static final Type<ClientBroomDismountInputPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_broom_dismount_input")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientBroomDismountInputPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeVarInt(packet.broomId);
                buffer.writeBoolean(packet.pressed);
            }, buffer -> new ClientBroomDismountInputPacket(
                    buffer.readVarInt(),
                    buffer.readBoolean()
            ));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientBroomDismountInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)
                    || !(sender.getVehicle() instanceof AbstractBroomEntity broom)
                    || broom.getId() != packet.broomId
                    || broom.getControllingPassenger() != sender) {
                return;
            }
            BroomDismountEvents.handleSneakInput(sender, broom, packet.pressed);
        });
    }
}
