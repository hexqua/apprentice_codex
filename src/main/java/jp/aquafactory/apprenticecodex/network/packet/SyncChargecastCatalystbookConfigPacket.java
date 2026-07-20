package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.config.item.ChargecastCatalystbookServerConfig;
import jp.aquafactory.apprenticecodex.item.chargecastcatalystbook.ChargecastCatalystbookClientConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
        buffer.writeVarInt(packet.values.spellDenylist().size());
        for (var spellId : packet.values.spellDenylist()) {
            buffer.writeResourceLocation(spellId);
        }
    }

    public static SyncChargecastCatalystbookConfigPacket decode(FriendlyByteBuf buffer) {
        var castTimeTicks = buffer.readVarInt();
        var spellPowerMultiplier = buffer.readDouble();
        var silverRingCastTimeBonusFactor = buffer.readDouble();
        var spellDenylistSize = buffer.readVarInt();
        var spellDenylist = new java.util.ArrayList<ResourceLocation>(spellDenylistSize);
        for (var index = 0; index < spellDenylistSize; ++index) {
            spellDenylist.add(buffer.readResourceLocation());
        }
        return new SyncChargecastCatalystbookConfigPacket(new ChargecastCatalystbookServerConfig.Values(
                castTimeTicks, spellPowerMultiplier, silverRingCastTimeBonusFactor, spellDenylist
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
