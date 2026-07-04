package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletSpellProfileManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class SyncAutocastAmuletProfileSpellIdsPacket {
    private final List<ResourceLocation> profileSpellIds;

    public SyncAutocastAmuletProfileSpellIdsPacket(List<ResourceLocation> profileSpellIds) {
        this.profileSpellIds = Collections.unmodifiableList(new ArrayList<>(profileSpellIds));
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
        for (int i = 0; i < size; i++) {
            profileSpellIds.add(buffer.readResourceLocation());
        }
        return new SyncAutocastAmuletProfileSpellIdsPacket(profileSpellIds);
    }

    public static void handle(
            SyncAutocastAmuletProfileSpellIdsPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
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
