package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.compat.epicfight.EpicFightSwingMagicCompat;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifle;
import jp.aquafactory.apprenticecodex.utility.BlockTargetData;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientFullautoRapidcastSpellrifleCastPacket(
        boolean adsFullAuto,
        BlockTargetData targetData
) implements CustomPacketPayload {
    public static final Type<ClientFullautoRapidcastSpellrifleCastPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_fullauto_rapidcast_spellrifle_cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientFullautoRapidcastSpellrifleCastPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), ClientFullautoRapidcastSpellrifleCastPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(ClientFullautoRapidcastSpellrifleCastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.adsFullAuto());
        packet.targetData().writeToBuffer(buffer);
    }

    public static ClientFullautoRapidcastSpellrifleCastPacket decode(FriendlyByteBuf buffer) {
        var adsFullAuto = buffer.readBoolean();
        var targetData = new BlockTargetData();
        targetData.readFromBuffer(buffer);
        return new ClientFullautoRapidcastSpellrifleCastPacket(adsFullAuto, targetData);
    }

    public static void handle(ClientFullautoRapidcastSpellrifleCastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender) || sender.isSpectator()) {
                return;
            }

            var mainHandItem = sender.getMainHandItem().getItem();
            if (mainHandItem instanceof FullautoRapidcastSpellrifle staffrifle) {
                var casted = staffrifle.tryTriggerSelectedSpell(sender, packet.adsFullAuto(), packet.targetData());
                if (casted && ModList.get().isLoaded(EpicFightSwingMagicCompat.MOD_ID)) {
                    EpicFightSwingMagicCompat.playStaffrifleShotAnimation(sender);
                }
            }
        });
    }
}
