package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.undyingemblem.UndyingEmblemConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SyncUndyingEmblemConfigPacket(int reconstructionSpeedMultiplier) {
    public static void encode(SyncUndyingEmblemConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.reconstructionSpeedMultiplier);
    }

    public static SyncUndyingEmblemConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncUndyingEmblemConfigPacket(buffer.readVarInt());
    }

    public static void handle(SyncUndyingEmblemConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
        private static void handle(SyncUndyingEmblemConfigPacket packet) {
            UndyingEmblemConfigState.set(packet.reconstructionSpeedMultiplier);
        }
    }
}
