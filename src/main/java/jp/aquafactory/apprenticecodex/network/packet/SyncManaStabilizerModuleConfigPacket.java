package jp.aquafactory.apprenticecodex.network.packet;

import java.util.function.Supplier;
import jp.aquafactory.apprenticecodex.config.item.ManaStabilizerModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerModuleConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

public record SyncManaStabilizerModuleConfigPacket(Values values) {
    public static void encode(SyncManaStabilizerModuleConfigPacket packet, FriendlyByteBuf buffer) { buffer.writeDouble(packet.values().adsMovementSpeedMultiplier()); buffer.writeDouble(packet.values().adsRecoilMultiplier()); buffer.writeDouble(packet.values().adsSpreadBonus()); buffer.writeDouble(packet.values().recoveryDegreesPerTick()); buffer.writeDouble(packet.values().manaCostPerTick()); }
    public static SyncManaStabilizerModuleConfigPacket decode(FriendlyByteBuf buffer) { return new SyncManaStabilizerModuleConfigPacket(new Values(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble())); }
    public static void handle(SyncManaStabilizerModuleConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> { if (FMLEnvironment.dist == Dist.CLIENT) ManaStabilizerModuleConfigState.set(packet.values()); });
        context.setPacketHandled(true);
    }
}
