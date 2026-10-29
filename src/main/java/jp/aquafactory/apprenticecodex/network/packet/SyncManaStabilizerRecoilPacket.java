package jp.aquafactory.apprenticecodex.network.packet;

import java.util.function.Supplier;
import jp.aquafactory.apprenticecodex.event.client.ManaStabilizerClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

public record SyncManaStabilizerRecoilPacket(long tick, long revision, long shot, float pitch, float yaw, boolean paid) {
    public static void encode(SyncManaStabilizerRecoilPacket packet, FriendlyByteBuf buffer) { buffer.writeLong(packet.tick()); buffer.writeVarLong(packet.revision()); buffer.writeVarLong(packet.shot()); buffer.writeFloat(packet.pitch()); buffer.writeFloat(packet.yaw()); buffer.writeBoolean(packet.paid()); }
    public static SyncManaStabilizerRecoilPacket decode(FriendlyByteBuf buffer) { return new SyncManaStabilizerRecoilPacket(buffer.readLong(), buffer.readVarLong(), buffer.readVarLong(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean()); }
    public static void handle(SyncManaStabilizerRecoilPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> { if (FMLEnvironment.dist == Dist.CLIENT && ModList.get().isLoaded("irons_artifice")) ClientHandler.accept(packet); });
        context.setPacketHandled(true);
    }
    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void accept(SyncManaStabilizerRecoilPacket packet) { ManaStabilizerClientEvents.accept(packet); }
    }
}
