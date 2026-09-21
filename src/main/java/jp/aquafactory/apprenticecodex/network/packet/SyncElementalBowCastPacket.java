package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowClientCastState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncElementalBowCastPacket(UUID playerId, String spellId, boolean active) {
    public static void encode(SyncElementalBowCastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeUtf(packet.spellId);
        buffer.writeBoolean(packet.active);
    }

    public static SyncElementalBowCastPacket decode(FriendlyByteBuf buffer) {
        return new SyncElementalBowCastPacket(buffer.readUUID(), buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(SyncElementalBowCastPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncElementalBowCastPacket packet) {
            ElementalBowClientCastState.sync(packet.playerId, packet.spellId, packet.active);
        }
    }
}
