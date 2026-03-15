package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import io.redspace.ironsspellbooks.api.util.Utils;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import jp.aquafactory.apprenticecodex.utility.BlockTargetingHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientBlockTargetCastPacket implements CustomPacketPayload {
    public static final Type<ClientBlockTargetCastPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_block_target_cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientBlockTargetCastPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientBlockTargetCastPacket::decode);

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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

    public static void handle(ClientBlockTargetCastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> handleOnServer(packet, (ServerPlayer) context.player()));
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
