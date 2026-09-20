package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.elementalbow.ElementalBowClientCastState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SyncElementalBowCastPacket(UUID playerId, String spellId, boolean active) {
    public static void encode(SyncElementalBowCastPacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeUtf(packet.spellId);
        buffer.writeBoolean(packet.active);
    }

    public static SyncElementalBowCastPacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
        return new SyncElementalBowCastPacket(buffer.readUUID(), buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(SyncElementalBowCastPacket packet, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> supplier) {
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
