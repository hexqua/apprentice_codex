package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowChargeSettings;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public final class SyncFocusStaffbowConfigPacket {
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

    public static void encode(SyncFocusStaffbowConfigPacket packet, FriendlyByteBuf buffer) {
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

    public static SyncFocusStaffbowConfigPacket decode(FriendlyByteBuf buffer) {
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

    public static void handle(SyncFocusStaffbowConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
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
