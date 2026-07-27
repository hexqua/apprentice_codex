package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.apprenticedesk.ApprenticeDeskFeatureState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncApprenticeDeskConfigPacket(
        boolean disableNonJobSiteFeatures,
        int commonInkMaxUses,
        int uncommonInkMaxUses,
        int rareInkMaxUses,
        int epicInkMaxUses,
        int legendaryInkMaxUses
) implements CustomPacketPayload {
    public static final Type<SyncApprenticeDeskConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_apprentice_desk_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncApprenticeDeskConfigPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncApprenticeDeskConfigPacket::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncApprenticeDeskConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.disableNonJobSiteFeatures());
        buffer.writeVarInt(packet.commonInkMaxUses());
        buffer.writeVarInt(packet.uncommonInkMaxUses());
        buffer.writeVarInt(packet.rareInkMaxUses());
        buffer.writeVarInt(packet.epicInkMaxUses());
        buffer.writeVarInt(packet.legendaryInkMaxUses());
    }

    private static SyncApprenticeDeskConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncApprenticeDeskConfigPacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt()
        );
    }

    public static void handle(SyncApprenticeDeskConfigPacket packet, IPayloadContext context) {
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

        private static void handle(SyncApprenticeDeskConfigPacket packet) {
            ApprenticeDeskFeatureState.setDisableNonJobSiteFeatures(packet.disableNonJobSiteFeatures());
            ApprenticeDeskFeatureState.setInkMaxUses(
                    packet.commonInkMaxUses(),
                    packet.uncommonInkMaxUses(),
                    packet.rareInkMaxUses(),
                    packet.epicInkMaxUses(),
                    packet.legendaryInkMaxUses()
            );
        }
    }
}
