package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientBlockTargetCastPacket {
    private final int quickCastSlot;
    private final ResourceLocation spellId;
    private final BlockTargetData targetData;
    private final boolean initiateCast;

    public ClientBlockTargetCastPacket(int quickCastSlot, ResourceLocation spellId, BlockTargetData targetData) {
        this(quickCastSlot, spellId, targetData, true);
    }

    public ClientBlockTargetCastPacket(int quickCastSlot, ResourceLocation spellId, BlockTargetData targetData, boolean initiateCast) {
        this.quickCastSlot = quickCastSlot;
        this.spellId = spellId;
        this.targetData = targetData == null ? new BlockTargetData() : targetData;
        this.initiateCast = initiateCast;
    }

    public static void encode(ClientBlockTargetCastPacket packet, FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeInt(packet.quickCastSlot);
        friendlyByteBuf.writeResourceLocation(packet.spellId);
        friendlyByteBuf.writeBoolean(packet.initiateCast);
        packet.targetData.writeToBuffer(friendlyByteBuf);
    }

    public static ClientBlockTargetCastPacket decode(FriendlyByteBuf friendlyByteBuf) {
        var quickCastSlot = friendlyByteBuf.readInt();
        var spellId = friendlyByteBuf.readResourceLocation();
        var initiateCast = friendlyByteBuf.readBoolean();
        var targetData = new BlockTargetData();
        targetData.readFromBuffer(friendlyByteBuf);
        return new ClientBlockTargetCastPacket(quickCastSlot, spellId, targetData, initiateCast);
    }

    public static void handle(ClientBlockTargetCastPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> handleOnServer(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void handleOnServer(ClientBlockTargetCastPacket packet, ServerPlayer serverPlayer) {
        if (serverPlayer == null) {
            return;
        }

        BlockTargetingHelper.setPendingServerTarget(serverPlayer, packet.spellId, packet.targetData);
        if (!packet.initiateCast) {
            return;
        }

        try {
            if (packet.quickCastSlot >= 0) {
                Utils.serverSideInitiateQuickCast(serverPlayer, packet.quickCastSlot);
            } else {
                Utils.serverSideInitiateCast(serverPlayer);
            }
        } finally {
            BlockTargetingHelper.clearPendingServerTarget(serverPlayer);
        }
    }
}
