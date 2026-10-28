package jp.aquafactory.apprenticecodex.network.packet;

import java.util.function.Supplier;
import jp.aquafactory.apprenticecodex.config.item.CommenceMagiModuleServerConfig.Values;
import jp.aquafactory.apprenticecodex.item.modifier.commencemagimodule.CommenceMagiModuleConfigState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

public record SyncCommenceMagiModuleConfigPacket(Values values) {
    public static void encode(SyncCommenceMagiModuleConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.values.initialHeadshotBonusPercent());
        buffer.writeVarInt(packet.values.additionalHeadshotBonusPercent());
        buffer.writeVarInt(packet.values.manaCost());
    }

    public static SyncCommenceMagiModuleConfigPacket decode(FriendlyByteBuf buffer) {
        return new SyncCommenceMagiModuleConfigPacket(new Values(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()));
    }

    public static void handle(SyncCommenceMagiModuleConfigPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
        private static void handle(SyncCommenceMagiModuleConfigPacket packet) {
            CommenceMagiModuleConfigState.set(packet.values);
        }
    }
}
