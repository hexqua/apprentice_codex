package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDeskFeatureState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncApprenticeDeskConfigPacket {
    private final boolean disableNonJobSiteFeatures;

    public SyncApprenticeDeskConfigPacket(boolean disableNonJobSiteFeatures) {
        this.disableNonJobSiteFeatures = disableNonJobSiteFeatures;
    }

    public static void encode(SyncApprenticeDeskConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.disableNonJobSiteFeatures);
    }

    public static SyncApprenticeDeskConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncApprenticeDeskConfigPacket(buffer.readBoolean());
    }

    public static void handle(SyncApprenticeDeskConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncApprenticeDeskConfigPacket packet) {
            ApprenticeDeskFeatureState.setDisableNonJobSiteFeatures(packet.disableNonJobSiteFeatures);
        }
    }
}
