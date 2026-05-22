package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.manashieldcharm.ManaShieldCharmConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncManaShieldCharmConfigPacket {
    private final float manaPerDamage;
    private final int recoveryThresholdMana;

    public SyncManaShieldCharmConfigPacket(float manaPerDamage, int recoveryThresholdMana) {
        this.manaPerDamage = manaPerDamage;
        this.recoveryThresholdMana = recoveryThresholdMana;
    }

    public static void encode(SyncManaShieldCharmConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.manaPerDamage);
        buffer.writeVarInt(packet.recoveryThresholdMana);
    }

    public static SyncManaShieldCharmConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaShieldCharmConfigPacket(
                buffer.readFloat(),
                buffer.readVarInt()
        );
    }

    public static void handle(SyncManaShieldCharmConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncManaShieldCharmConfigPacket packet) {
            ManaShieldCharmConfigState.set(packet.manaPerDamage, packet.recoveryThresholdMana);
        }
    }
}
