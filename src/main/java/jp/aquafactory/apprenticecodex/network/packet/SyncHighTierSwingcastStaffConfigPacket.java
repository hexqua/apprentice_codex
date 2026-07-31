package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.swingstaff.HighTierSwingcastStaffConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public final class SyncHighTierSwingcastStaffConfigPacket implements CustomPacketPayload {
    public static final Type<SyncHighTierSwingcastStaffConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID,
                    "sync_high_tier_swingcast_staff_config"
            ));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncHighTierSwingcastStaffConfigPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, packet) -> encode(packet, buffer),
                    SyncHighTierSwingcastStaffConfigPacket::decode
            );

    private final int diamondCooldownReductionTicks;
    private final int netheriteCooldownReductionTicks;

    public SyncHighTierSwingcastStaffConfigPacket(
            int diamondCooldownReductionTicks,
            int netheriteCooldownReductionTicks
    ) {
        this.diamondCooldownReductionTicks = diamondCooldownReductionTicks;
        this.netheriteCooldownReductionTicks = netheriteCooldownReductionTicks;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncHighTierSwingcastStaffConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.diamondCooldownReductionTicks);
        buffer.writeVarInt(packet.netheriteCooldownReductionTicks);
    }

    private static SyncHighTierSwingcastStaffConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncHighTierSwingcastStaffConfigPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(SyncHighTierSwingcastStaffConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncHighTierSwingcastStaffConfigPacket packet) {
            HighTierSwingcastStaffConfigState.setCooldownReductionTicks(
                    packet.diamondCooldownReductionTicks,
                    packet.netheriteCooldownReductionTicks
            );
        }
    }
}
