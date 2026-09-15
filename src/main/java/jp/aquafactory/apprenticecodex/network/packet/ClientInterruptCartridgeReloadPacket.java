package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.quickcastscrollcartridge.QuickcastCartridgeCharge;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ClientInterruptCartridgeReloadPacket() implements CustomPacketPayload {
    public static final Type<ClientInterruptCartridgeReloadPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "interrupt_cartridge_reload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientInterruptCartridgeReloadPacket> STREAM_CODEC =
            StreamCodec.unit(new ClientInterruptCartridgeReloadPacket());
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(ClientInterruptCartridgeReloadPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) QuickcastCartridgeCharge.interruptReload(player);
        });
    }
}
