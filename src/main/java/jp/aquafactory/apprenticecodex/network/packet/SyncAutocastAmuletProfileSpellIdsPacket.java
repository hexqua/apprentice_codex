package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileManager;
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

import java.util.ArrayList;
import java.util.List;

public record SyncAutocastAmuletProfileSpellIdsPacket(List<ResourceLocation> profileSpellIds)
        implements CustomPacketPayload {
    public static final Type<SyncAutocastAmuletProfileSpellIdsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_autocast_amulet_profile_spell_ids"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAutocastAmuletProfileSpellIdsPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncAutocastAmuletProfileSpellIdsPacket::decode);

    public SyncAutocastAmuletProfileSpellIdsPacket {
        profileSpellIds = List.copyOf(profileSpellIds);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncAutocastAmuletProfileSpellIdsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.profileSpellIds.size());
        for (var spellId : packet.profileSpellIds) {
            buffer.writeResourceLocation(spellId);
        }
    }

    public static SyncAutocastAmuletProfileSpellIdsPacket decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        var profileSpellIds = new ArrayList<ResourceLocation>(size);
        for (var i = 0; i < size; ++i) {
            profileSpellIds.add(buffer.readResourceLocation());
        }
        return new SyncAutocastAmuletProfileSpellIdsPacket(profileSpellIds);
    }

    public static void handle(SyncAutocastAmuletProfileSpellIdsPacket packet, IPayloadContext context) {
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

        private static void handle(SyncAutocastAmuletProfileSpellIdsPacket packet) {
            AutocastAmuletSpellProfileManager.applyClientSyncedProfileSpellIds(packet.profileSpellIds);
        }
    }
}
