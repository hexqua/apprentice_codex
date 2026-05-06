package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphonClientRenderState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncPhotonSiphonCombatStatePacket {
    private final boolean inCombat;

    public SyncPhotonSiphonCombatStatePacket(boolean inCombat) {
        this.inCombat = inCombat;
    }

    public static void encode(SyncPhotonSiphonCombatStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.inCombat);
    }

    public static SyncPhotonSiphonCombatStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncPhotonSiphonCombatStatePacket(buffer.readBoolean());
    }

    public static void handle(SyncPhotonSiphonCombatStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncPhotonSiphonCombatStatePacket packet) {
            PhotonSiphonClientRenderState.setSyncedCombatState(packet.inCombat);
        }
    }
}
