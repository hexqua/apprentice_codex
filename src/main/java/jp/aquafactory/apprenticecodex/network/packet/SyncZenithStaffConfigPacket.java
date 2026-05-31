package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.zenithstaff.ZenithStaffConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SyncZenithStaffConfigPacket implements CustomPacketPayload {
    public static final Type<SyncZenithStaffConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_zenith_staff_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncZenithStaffConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncZenithStaffConfigPacket::decode);

    private final float manaCostMultiplier;

    public SyncZenithStaffConfigPacket(float manaCostMultiplier) {
        this.manaCostMultiplier = manaCostMultiplier;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncZenithStaffConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.manaCostMultiplier);
    }

    private static SyncZenithStaffConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncZenithStaffConfigPacket(buffer.readFloat());
    }

    public static void handle(SyncZenithStaffConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncZenithStaffConfigPacket packet) {
            ZenithStaffConfigState.setManaCostMultiplier(packet.manaCostMultiplier);
        }
    }
}
