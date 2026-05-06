package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.offhand.PhotonSiphonClientRenderState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SyncPhotonSiphonCombatStatePacket implements CustomPacketPayload {
    public static final Type<SyncPhotonSiphonCombatStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_photon_siphon_combat_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPhotonSiphonCombatStatePacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncPhotonSiphonCombatStatePacket::decode);

    private final boolean inCombat;

    public SyncPhotonSiphonCombatStatePacket(boolean inCombat) {
        this.inCombat = inCombat;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(SyncPhotonSiphonCombatStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.inCombat);
    }

    private static SyncPhotonSiphonCombatStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncPhotonSiphonCombatStatePacket(buffer.readBoolean());
    }

    public static void handle(SyncPhotonSiphonCombatStatePacket packet, IPayloadContext context) {
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

        private static void handle(SyncPhotonSiphonCombatStatePacket packet) {
            PhotonSiphonClientRenderState.setSyncedCombatState(packet.inCombat);
        }
    }
}
