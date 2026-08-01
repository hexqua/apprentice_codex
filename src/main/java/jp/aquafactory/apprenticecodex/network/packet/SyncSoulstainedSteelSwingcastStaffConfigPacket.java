package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.swingstaff.SoulstainedSteelSwingcastStaffConfigState;
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

public final class SyncSoulstainedSteelSwingcastStaffConfigPacket implements CustomPacketPayload {
    public static final Type<SyncSoulstainedSteelSwingcastStaffConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    ApprenticeCodex.MODID,
                    "sync_soulstained_steel_swingcast_staff_config"
            )
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSoulstainedSteelSwingcastStaffConfigPacket>
            STREAM_CODEC = StreamCodec.of(
                    (buffer, packet) -> encode(packet, buffer),
                    SyncSoulstainedSteelSwingcastStaffConfigPacket::decode
            );

    private final double manaCostPerBlade;

    public SyncSoulstainedSteelSwingcastStaffConfigPacket(double manaCostPerBlade) {
        this.manaCostPerBlade = manaCostPerBlade;
    }

    public double manaCostPerBlade() {
        return manaCostPerBlade;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncSoulstainedSteelSwingcastStaffConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.manaCostPerBlade);
    }

    public static SyncSoulstainedSteelSwingcastStaffConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncSoulstainedSteelSwingcastStaffConfigPacket(buffer.readDouble());
    }

    public static void handle(SyncSoulstainedSteelSwingcastStaffConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncSoulstainedSteelSwingcastStaffConfigPacket packet) {
            SoulstainedSteelSwingcastStaffConfigState.setManaCostPerBlade(packet.manaCostPerBlade);
        }
    }
}
