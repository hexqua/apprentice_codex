package jp.aquafactory.apprenticecodex.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncScarletThirstHealthPacket {
    private final float health;

    public SyncScarletThirstHealthPacket(float health) {
        this.health = health;
    }

    public static void encode(SyncScarletThirstHealthPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.health);
    }

    public static SyncScarletThirstHealthPacket decode(FriendlyByteBuf buffer) {
        return new SyncScarletThirstHealthPacket(buffer.readFloat());
    }

    public static void handle(SyncScarletThirstHealthPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncScarletThirstHealthPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            player.setHealth(packet.health);
        }
    }
}
