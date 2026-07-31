package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.swingstaff.IronSwingcastStaffConfigState;
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

public final class SyncIronSwingcastStaffConfigPacket implements CustomPacketPayload {
    public static final Type<SyncIronSwingcastStaffConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_iron_swingcast_staff_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncIronSwingcastStaffConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncIronSwingcastStaffConfigPacket::decode);

    private final double crystallineArcaneShardDropChance;

    public SyncIronSwingcastStaffConfigPacket(double crystallineArcaneShardDropChance) {
        this.crystallineArcaneShardDropChance = crystallineArcaneShardDropChance;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncIronSwingcastStaffConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.crystallineArcaneShardDropChance);
    }

    private static SyncIronSwingcastStaffConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncIronSwingcastStaffConfigPacket(buffer.readDouble());
    }

    public static void handle(SyncIronSwingcastStaffConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncIronSwingcastStaffConfigPacket packet) {
            IronSwingcastStaffConfigState.setCrystallineArcaneShardDropChance(
                    packet.crystallineArcaneShardDropChance
            );
        }
    }
}
