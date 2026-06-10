package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.manathruster.ManaThrusterConfigState;
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

public final class SyncManaThrusterConfigPacket implements CustomPacketPayload {
    public static final Type<SyncManaThrusterConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_mana_thruster_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaThrusterConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncManaThrusterConfigPacket::decode);
    private final float manaCostPerTick;

    public SyncManaThrusterConfigPacket(float manaCostPerTick) {
        this.manaCostPerTick = manaCostPerTick;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncManaThrusterConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.manaCostPerTick);
    }

    public static SyncManaThrusterConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncManaThrusterConfigPacket(buffer.readFloat());
    }

    public static void handle(SyncManaThrusterConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncManaThrusterConfigPacket packet) {
            ManaThrusterConfigState.set(packet.manaCostPerTick);
        }
    }
}
