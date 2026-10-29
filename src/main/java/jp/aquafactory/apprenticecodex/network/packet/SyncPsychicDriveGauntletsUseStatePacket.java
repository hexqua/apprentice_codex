package jp.aquafactory.apprenticecodex.network.packet;

import java.util.function.Supplier;

import jp.aquafactory.apprenticecodex.event.client.PsychicDriveGauntletsUseInput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

public record SyncPsychicDriveGauntletsUseStatePacket(long session, boolean charging) {
    public static void encode(SyncPsychicDriveGauntletsUseStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.session);
        buffer.writeBoolean(packet.charging);
    }

    public static SyncPsychicDriveGauntletsUseStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncPsychicDriveGauntletsUseStatePacket(buffer.readLong(), buffer.readBoolean());
    }

    public static void handle(SyncPsychicDriveGauntletsUseStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.accept(packet);
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void accept(SyncPsychicDriveGauntletsUseStatePacket packet) {
            PsychicDriveGauntletsUseInput.accept(packet.session, packet.charging);
        }
    }
}
