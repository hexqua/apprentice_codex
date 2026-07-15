package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat;
import jp.aquafactory.apprenticecodex.item.multipurposestaffrifle.MultipurposeStaffrifle;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientMultipurposeStaffrifleCastPacket(
        boolean adsFullAuto,
        BlockTargetData targetData
) implements CustomPacketPayload {
    public static final Type<ClientMultipurposeStaffrifleCastPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_multipurpose_staffrifle_cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientMultipurposeStaffrifleCastPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientMultipurposeStaffrifleCastPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientMultipurposeStaffrifleCastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.adsFullAuto());
        packet.targetData().writeToBuffer(buffer);
    }

    public static ClientMultipurposeStaffrifleCastPacket decode(FriendlyByteBuf buffer) {
        var adsFullAuto = buffer.readBoolean();
        var targetData = new BlockTargetData();
        targetData.readFromBuffer(buffer);
        return new ClientMultipurposeStaffrifleCastPacket(adsFullAuto, targetData);
    }

    public static void handle(ClientMultipurposeStaffrifleCastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            var mainHandItem = sender.getMainHandItem().getItem();
            if (mainHandItem instanceof MultipurposeStaffrifle staffrifle) {
                var casted = staffrifle.tryTriggerSelectedSpell(sender, packet.adsFullAuto(), packet.targetData());
                if (casted && ModList.get().isLoaded(EpicFightSwingMagicCompat.MOD_ID)) {
                    EpicFightSwingMagicCompat.playStaffrifleShotAnimation(sender);
                }
            }
        });
    }
}
