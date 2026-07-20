package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.config.item.ChargecastCatalystbookServerConfig;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookClientConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncChargecastCatalystbookConfigPacket(ChargecastCatalystbookServerConfig.Values values) {
    public static void encode(SyncChargecastCatalystbookConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.values.castTimeTicks());
        buffer.writeDouble(packet.values.spellPowerMultiplier());
        buffer.writeDouble(packet.values.silverRingCastTimeBonusFactor());
    }

    public static SyncChargecastCatalystbookConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncChargecastCatalystbookConfigPacket(new ChargecastCatalystbookServerConfig.Values(
                buffer.readVarInt(), buffer.readDouble(), buffer.readDouble()
        ));
    }

    public static void handle(SyncChargecastCatalystbookConfigPacket packet,
                              Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ClientHandler.handle(packet)
        ));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncChargecastCatalystbookConfigPacket packet) {
            ChargecastCatalystbookClientConfigState.set(packet.values);
        }
    }
}
