package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule.CommenceMagiModuleConfigState;
import jp.aquafactory.apprenticecodex.config.item.CommenceMagiModuleServerConfig.Values;
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

public record SyncCommenceMagiModuleConfigPacket(Values values) implements CustomPacketPayload {
    public static final Type<SyncCommenceMagiModuleConfigPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_commence_magi_module_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncCommenceMagiModuleConfigPacket> STREAM_CODEC =
            StreamCodec.of(SyncCommenceMagiModuleConfigPacket::encode, SyncCommenceMagiModuleConfigPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(FriendlyByteBuf buffer, SyncCommenceMagiModuleConfigPacket packet) {
        buffer.writeVarInt(packet.values.initialHeadshotBonusPercent());
        buffer.writeVarInt(packet.values.additionalHeadshotBonusPercent());
        buffer.writeVarInt(packet.values.manaCost());
    }

    private static SyncCommenceMagiModuleConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncCommenceMagiModuleConfigPacket(new Values(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));
    }

    public static void handle(SyncCommenceMagiModuleConfigPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncCommenceMagiModuleConfigPacket packet) {
            CommenceMagiModuleConfigState.set(packet.values);
        }
    }
}
