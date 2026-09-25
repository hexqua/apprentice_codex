package jp.aquafactory.apprenticecodex.network.packet;

import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.multicastechostaff.MulticastEchoStaffAttackProfileManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;

public record SyncEchoProfileSpellIdsPacket(List<ResourceLocation> profileSpellIds)
        {
    public SyncEchoProfileSpellIdsPacket {
        profileSpellIds = List.copyOf(profileSpellIds);
    }


    public static void encode(SyncEchoProfileSpellIdsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.profileSpellIds.size());
        for (var spellId : packet.profileSpellIds) {
            buffer.writeResourceLocation(spellId);
        }
    }

    public static SyncEchoProfileSpellIdsPacket decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        var profileSpellIds = new ArrayList<ResourceLocation>(size);
        for (var i = 0; i < size; ++i) {
            profileSpellIds.add(buffer.readResourceLocation());
        }
        return new SyncEchoProfileSpellIdsPacket(profileSpellIds);
    }

    public static void handle(SyncEchoProfileSpellIdsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncEchoProfileSpellIdsPacket packet) {
            MulticastEchoStaffAttackProfileManager.applyClientSyncedProfileSpellIds(packet.profileSpellIds);
        }
    }
}
