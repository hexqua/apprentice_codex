package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class ClientMirageAvoidanceCastPacket implements CustomPacketPayload {
    public static final Type<ClientMirageAvoidanceCastPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_mirage_avoidance_cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMirageAvoidanceCastPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientMirageAvoidanceCastPacket::decode);
    private final int quickCastSlot;
    private final float forward;
    private final float strafe;
    private final boolean initiateCast;

    public ClientMirageAvoidanceCastPacket(int quickCastSlot, float forward, float strafe) {
        this(quickCastSlot, forward, strafe, true);
    }

    public ClientMirageAvoidanceCastPacket(int quickCastSlot, float forward, float strafe, boolean initiateCast) {
        this.quickCastSlot = quickCastSlot;
        this.forward = forward;
        this.strafe = strafe;
        this.initiateCast = initiateCast;
    }

    public static ClientMirageAvoidanceCastPacket rememberInput(float forward, float strafe) {
        return new ClientMirageAvoidanceCastPacket(-1, forward, strafe, false);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientMirageAvoidanceCastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.quickCastSlot);
        buffer.writeFloat(packet.forward);
        buffer.writeFloat(packet.strafe);
        buffer.writeBoolean(packet.initiateCast);
    }

    public static ClientMirageAvoidanceCastPacket decode(FriendlyByteBuf buffer) {
        return new ClientMirageAvoidanceCastPacket(
                buffer.readInt(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean()
        );
    }

    public static void handle(ClientMirageAvoidanceCastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer) || serverPlayer.isSpectator()) {
                return;
            }
            handleOnServer(packet, serverPlayer);
        });
    }

    private static void handleOnServer(ClientMirageAvoidanceCastPacket packet, ServerPlayer serverPlayer) {
        MirageAvoidanceInput.setPending(serverPlayer, packet.forward, packet.strafe);
        if (!packet.initiateCast) {
            return;
        }

        var initiated = packet.quickCastSlot >= 0
                ? Utils.serverSideInitiateQuickCast(serverPlayer, packet.quickCastSlot)
                : Utils.serverSideInitiateCast(serverPlayer);
        if (!initiated) {
            MirageAvoidanceInput.clearPending(serverPlayer);
        }
    }
}
