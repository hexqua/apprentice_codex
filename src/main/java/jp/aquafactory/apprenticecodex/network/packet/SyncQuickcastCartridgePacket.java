package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.event.client.QuickcastCartridgeClientState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;

public record SyncQuickcastCartridgePacket(boolean equipped, boolean available, boolean reserved, long serverTime,
        long recoveryUntil, long recoveryDuration, long reloadUntil, long reloadDuration, boolean completed)
        {
    public static void encode(SyncQuickcastCartridgePacket packet, net.minecraft.network.FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.equipped);
        buffer.writeBoolean(packet.available);
        buffer.writeBoolean(packet.reserved);
        buffer.writeLong(packet.serverTime);
        buffer.writeLong(packet.recoveryUntil);
        buffer.writeLong(packet.recoveryDuration);
        buffer.writeLong(packet.reloadUntil);
        buffer.writeLong(packet.reloadDuration);
        buffer.writeBoolean(packet.completed);
    }

    public static SyncQuickcastCartridgePacket decode(net.minecraft.network.FriendlyByteBuf buffer) {
        return new SyncQuickcastCartridgePacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
            buffer.readLong(), buffer.readLong(), buffer.readLong(), buffer.readLong(), buffer.readLong(), buffer.readBoolean());
    }


    public static void handle(SyncQuickcastCartridgePacket packet, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> supplier) {
        var context = supplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientHandler.handle(packet);
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        static void handle(SyncQuickcastCartridgePacket packet) { QuickcastCartridgeClientState.accept(packet); }
    }
}
