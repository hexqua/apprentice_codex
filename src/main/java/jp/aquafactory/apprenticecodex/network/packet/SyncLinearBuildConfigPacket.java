package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.spell.linearbuild.LinearBuildConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncLinearBuildConfigPacket(int manaCostPerBlock) {
    public static void encode(SyncLinearBuildConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.manaCostPerBlock);
    }

    public static SyncLinearBuildConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncLinearBuildConfigPacket(buffer.readVarInt());
    }

    public static void handle(
            SyncLinearBuildConfigPacket packet,
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

        private static void handle(SyncLinearBuildConfigPacket packet) {
            LinearBuildConfigState.set(packet.manaCostPerBlock);
        }
    }
}
