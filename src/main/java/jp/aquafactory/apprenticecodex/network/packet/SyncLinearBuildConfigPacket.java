package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildConfigState;
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

public record SyncLinearBuildConfigPacket(int manaCostPerBlock) implements CustomPacketPayload {
    public static final Type<SyncLinearBuildConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_linear_build_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLinearBuildConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncLinearBuildConfigPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncLinearBuildConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.manaCostPerBlock);
    }

    public static SyncLinearBuildConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncLinearBuildConfigPacket(buffer.readVarInt());
    }

    public static void handle(SyncLinearBuildConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncLinearBuildConfigPacket packet) {
            LinearBuildConfigState.set(packet.manaCostPerBlock);
        }
    }
}
