package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.undyingemblem.UndyingEmblemClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SyncUndyingEmblemStatePacket(int remainingCooldownTicks, long serverGameTime) {
    public static void encode(SyncUndyingEmblemStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.remainingCooldownTicks);
        buffer.writeLong(packet.serverGameTime);
    }

    public static SyncUndyingEmblemStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncUndyingEmblemStatePacket(buffer.readVarInt(), buffer.readLong());
    }

    public static void handle(SyncUndyingEmblemStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
        private static void handle(SyncUndyingEmblemStatePacket packet) {
            UndyingEmblemClientState.set(packet.remainingCooldownTicks, packet.serverGameTime);
        }
    }
}
