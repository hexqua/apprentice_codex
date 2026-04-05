package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.isekaitravelguidebook.IsekaiTravelGuidebookTooltipState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncIsekaiTravelGuidebookConfigPacket {
    private final boolean showTooltip;

    public SyncIsekaiTravelGuidebookConfigPacket(boolean showTooltip) {
        this.showTooltip = showTooltip;
    }

    public static void encode(SyncIsekaiTravelGuidebookConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.showTooltip);
    }

    public static SyncIsekaiTravelGuidebookConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncIsekaiTravelGuidebookConfigPacket(buffer.readBoolean());
    }

    public static void handle(SyncIsekaiTravelGuidebookConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncIsekaiTravelGuidebookConfigPacket packet) {
            IsekaiTravelGuidebookTooltipState.setShowTooltip(packet.showTooltip);
        }
    }
}
