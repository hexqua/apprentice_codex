package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class SyncManaForceBladeConfigPacket {
    private final float imbueDamageMultiplierScale;
    private final float attackManaCostMultiplier;
    private final float attackManaSchoolMultiplierScale;

    public SyncManaForceBladeConfigPacket(
            float imbueDamageMultiplierScale,
            float attackManaCostMultiplier,
            float attackManaSchoolMultiplierScale
    ) {
        this.imbueDamageMultiplierScale = imbueDamageMultiplierScale;
        this.attackManaCostMultiplier = attackManaCostMultiplier;
        this.attackManaSchoolMultiplierScale = attackManaSchoolMultiplierScale;
    }

    public static void encode(SyncManaForceBladeConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.imbueDamageMultiplierScale);
        buffer.writeFloat(packet.attackManaCostMultiplier);
        buffer.writeFloat(packet.attackManaSchoolMultiplierScale);
    }

    public static SyncManaForceBladeConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaForceBladeConfigPacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(SyncManaForceBladeConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncManaForceBladeConfigPacket packet) {
            ManaForceBladeConfigState.set(
                    packet.imbueDamageMultiplierScale,
                    packet.attackManaCostMultiplier,
                    packet.attackManaSchoolMultiplierScale
            );
        }
    }
}
