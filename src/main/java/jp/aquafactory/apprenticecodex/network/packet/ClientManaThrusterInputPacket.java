package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterFlightManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientManaThrusterInputPacket(boolean active, float strafeInput, float forwardInput)
        implements CustomPacketPayload {
    public static final Type<ClientManaThrusterInputPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_mana_thruster_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientManaThrusterInputPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientManaThrusterInputPacket::decode);

    public ClientManaThrusterInputPacket {
        strafeInput = active ? sanitizeInput(strafeInput) : 0.0F;
        forwardInput = active ? sanitizeInput(forwardInput) : 0.0F;
    }

    public static ClientManaThrusterInputPacket inactive() {
        return new ClientManaThrusterInputPacket(false, 0.0F, 0.0F);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static float sanitizeInput(float input) {
        return Float.isFinite(input) ? Mth.clamp(input, -1.0F, 1.0F) : 0.0F;
    }

    public static void encode(ClientManaThrusterInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeFloat(packet.strafeInput);
        buffer.writeFloat(packet.forwardInput);
    }

    public static ClientManaThrusterInputPacket decode(FriendlyByteBuf buffer) {
        return new ClientManaThrusterInputPacket(buffer.readBoolean(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(ClientManaThrusterInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            ManaThrusterFlightManager.setJumpInput(sender, packet.active, packet.strafeInput, packet.forwardInput);
        });
    }
}
