package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SyncCircuitHeatStaffConfigPacket implements CustomPacketPayload {
    public static final Type<SyncCircuitHeatStaffConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_circuit_heat_staff_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCircuitHeatStaffConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncCircuitHeatStaffConfigPacket::decode);
    private final int cooldownBypassMaxRemainingTicks;

    public SyncCircuitHeatStaffConfigPacket(int cooldownBypassMaxRemainingTicks) {
        this.cooldownBypassMaxRemainingTicks = cooldownBypassMaxRemainingTicks;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncCircuitHeatStaffConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.cooldownBypassMaxRemainingTicks);
    }

    private static SyncCircuitHeatStaffConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncCircuitHeatStaffConfigPacket(buffer.readVarInt());
    }

    public static void handle(SyncCircuitHeatStaffConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncCircuitHeatStaffConfigPacket packet) {
            CircuitHeatStaffConfigState.setCooldownBypassMaxRemainingTicks(packet.cooldownBypassMaxRemainingTicks);
        }
    }
}
