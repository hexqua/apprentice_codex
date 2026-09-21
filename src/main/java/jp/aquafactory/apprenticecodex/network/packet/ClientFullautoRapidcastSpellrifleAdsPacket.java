package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle.FullautoRapidcastSpellrifleAdsMovement;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientFullautoRapidcastSpellrifleAdsPacket(boolean aiming) implements CustomPacketPayload {
    public static final Type<ClientFullautoRapidcastSpellrifleAdsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "client_fullauto_rapidcast_spellrifle_ads"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientFullautoRapidcastSpellrifleAdsPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> buffer.writeBoolean(packet.aiming()),
                    buffer -> new ClientFullautoRapidcastSpellrifleAdsPacket(buffer.readBoolean()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClientFullautoRapidcastSpellrifleAdsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                FullautoRapidcastSpellrifleAdsMovement.update(player, packet.aiming());
            }
        });
    }
}
