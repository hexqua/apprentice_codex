package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.spell.mirageavoidance.MirageAvoidanceInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientMirageAvoidanceCastPacket {
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

    public static void handle(ClientMirageAvoidanceCastPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> handleOnServer(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void handleOnServer(ClientMirageAvoidanceCastPacket packet, ServerPlayer serverPlayer) {
        if (serverPlayer == null) {
            return;
        }

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
