package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientBroomInputPacket(
        float strafeInput,
        float forwardInput,
        boolean ascending,
        boolean descending
) implements CustomPacketPayload {
    public static final Type<ClientBroomInputPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_broom_input")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientBroomInputPacket> STREAM_CODEC =
            StreamCodec.of(ClientBroomInputPacket::encode, ClientBroomInputPacket::decode);

    public ClientBroomInputPacket {
        strafeInput = sanitizeInput(strafeInput);
        forwardInput = sanitizeInput(forwardInput);
    }

    public static ClientBroomInputPacket inactive() {
        return new ClientBroomInputPacket(0.0F, 0.0F, false, false);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static float sanitizeInput(float input) {
        return Float.isFinite(input) ? Mth.clamp(input, -1.0F, 1.0F) : 0.0F;
    }

    private static void encode(FriendlyByteBuf buffer, ClientBroomInputPacket packet) {
        buffer.writeFloat(packet.strafeInput);
        buffer.writeFloat(packet.forwardInput);
        buffer.writeBoolean(packet.ascending);
        buffer.writeBoolean(packet.descending);
    }

    private static ClientBroomInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientBroomInputPacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(ClientBroomInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)
                    || !(sender.getVehicle() instanceof AbstractBroomEntity broom)
                    || broom.getControllingPassenger() != sender) {
                return;
            }
            broom.acceptServerInput(sender, packet.strafeInput, packet.forwardInput,
                    packet.ascending, packet.descending);
        });
    }
}
