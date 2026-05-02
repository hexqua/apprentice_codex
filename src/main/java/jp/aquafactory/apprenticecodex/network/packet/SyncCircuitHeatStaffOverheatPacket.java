package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.circuitheatstaff.CircuitHeatStaffOverheatManager;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public class SyncCircuitHeatStaffOverheatPacket implements CustomPacketPayload {
    public static final Type<SyncCircuitHeatStaffOverheatPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_circuit_heat_staff_overheat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCircuitHeatStaffOverheatPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncCircuitHeatStaffOverheatPacket::decode);

    private final CompoundTag data;

    public SyncCircuitHeatStaffOverheatPacket(@Nullable CompoundTag data) {
        this.data = data == null ? new CompoundTag() : data.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncCircuitHeatStaffOverheatPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.data);
    }

    public static SyncCircuitHeatStaffOverheatPacket decode(FriendlyByteBuf buffer) {
        return new SyncCircuitHeatStaffOverheatPacket(buffer.readNbt());
    }

    public static void handle(SyncCircuitHeatStaffOverheatPacket packet, IPayloadContext context) {
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

        private static void handle(SyncCircuitHeatStaffOverheatPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            CircuitHeatStaffOverheatManager.applySyncedState(player, packet.data);
        }
    }
}
