package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import jp.aquafactory.apprenticecodex.config.item.ManaStabilizerModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.modifier.manastabilizermodule.ManaStabilizerModuleConfigState;

public record SyncManaStabilizerModuleConfigPacket(Values values) implements CustomPacketPayload {
    public static final Type<SyncManaStabilizerModuleConfigPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mana_stabilizer_module_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaStabilizerModuleConfigPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> { buffer.writeDouble(packet.values().adsMovementSpeedMultiplier()); buffer.writeDouble(packet.values().adsRecoilMultiplier()); buffer.writeDouble(packet.values().adsSpreadBonus()); buffer.writeDouble(packet.values().recoveryDegreesPerTick()); buffer.writeDouble(packet.values().manaCostPerTick()); },
            buffer -> new SyncManaStabilizerModuleConfigPacket(new Values(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble())));
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncManaStabilizerModuleConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ManaStabilizerModuleConfigState.set(packet.values()));
    }
}

