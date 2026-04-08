package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SyncTamersPocketCountPacket implements CustomPacketPayload {
    public static final Type<SyncTamersPocketCountPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_tamers_pocket_count"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTamersPocketCountPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncTamersPocketCountPacket::decode);

    private final int storedPetCount;

    public SyncTamersPocketCountPacket(int storedPetCount) {
        this.storedPetCount = storedPetCount;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncTamersPocketCountPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.storedPetCount);
    }

    public static SyncTamersPocketCountPacket decode(FriendlyByteBuf buffer) {
        return new SyncTamersPocketCountPacket(buffer.readVarInt());
    }

    public static void handle(SyncTamersPocketCountPacket packet, IPayloadContext context) {
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

        private static void handle(SyncTamersPocketCountPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            Capabilities.getSpellData(player).ifPresent(data ->
                    data.edit(CodexSpellStateTypeRegister.TAMERS_POCKET_STATE, state ->
                            state.setClientSyncedStoredPetCount(packet.storedPetCount)
                    )
            );
        }
    }
}
