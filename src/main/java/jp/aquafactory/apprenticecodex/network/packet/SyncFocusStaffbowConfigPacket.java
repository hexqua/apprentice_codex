package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeSettings;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public final class SyncFocusStaffbowConfigPacket implements CustomPacketPayload {
    public static final Type<SyncFocusStaffbowConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_focus_staffbow_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncFocusStaffbowConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncFocusStaffbowConfigPacket::decode);
    private final boolean continuousFocusedCastEnabled;
    private final boolean arrowCatalystRequired;
    private final List<ResourceLocation> arrowCatalystItemIds;
    private final FocusStaffbowChargeSettings chargeSettings;

    public SyncFocusStaffbowConfigPacket(
            boolean continuousFocusedCastEnabled,
            boolean arrowCatalystRequired,
            List<ResourceLocation> arrowCatalystItemIds,
            FocusStaffbowChargeSettings chargeSettings
    ) {
        this.continuousFocusedCastEnabled = continuousFocusedCastEnabled;
        this.arrowCatalystRequired = arrowCatalystRequired;
        this.arrowCatalystItemIds = List.copyOf(arrowCatalystItemIds);
        this.chargeSettings = chargeSettings;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncFocusStaffbowConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.continuousFocusedCastEnabled);
        buffer.writeBoolean(packet.arrowCatalystRequired);
        buffer.writeVarInt(packet.arrowCatalystItemIds.size());
        for (var itemId : packet.arrowCatalystItemIds) {
            buffer.writeResourceLocation(itemId);
        }
        buffer.writeDouble(packet.chargeSettings.pendingMaxChargeMultiplier());
        buffer.writeDouble(packet.chargeSettings.continuousMaxChargeMultiplier());
        buffer.writeVarInt(packet.chargeSettings.minimumOverchargeBaselineTicks());
        buffer.writeDouble(packet.chargeSettings.chargeManaCostExponent());
        buffer.writeDouble(packet.chargeSettings.chargeManaCostMultiplier());
    }

    private static SyncFocusStaffbowConfigPacket decode(FriendlyByteBuf buffer) {
        var continuousFocusedCastEnabled = buffer.readBoolean();
        var arrowCatalystRequired = buffer.readBoolean();
        var arrowCatalystItemCount = buffer.readVarInt();
        var arrowCatalystItemIds = new java.util.ArrayList<ResourceLocation>(arrowCatalystItemCount);
        for (var index = 0; index < arrowCatalystItemCount; ++index) {
            arrowCatalystItemIds.add(buffer.readResourceLocation());
        }
        return new SyncFocusStaffbowConfigPacket(
                continuousFocusedCastEnabled,
                arrowCatalystRequired,
                arrowCatalystItemIds,
                new FocusStaffbowChargeSettings(
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readVarInt(),
                        buffer.readDouble(),
                        buffer.readDouble()
                )
        );
    }

    public static void handle(SyncFocusStaffbowConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncFocusStaffbowConfigPacket packet) {
            FocusStaffbowClientConfigState.set(
                    packet.continuousFocusedCastEnabled,
                    packet.arrowCatalystRequired,
                    packet.arrowCatalystItemIds,
                    packet.chargeSettings
            );
        }
    }
}
