package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffRiptide;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SyncChargedStaffRiptidePacket(int entityId, boolean active, boolean maintenanceInput) {
    public static void encode(SyncChargedStaffRiptidePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeBoolean(packet.active);
        buffer.writeBoolean(packet.maintenanceInput);
    }

    public static SyncChargedStaffRiptidePacket decode(FriendlyByteBuf buffer) {
        return new SyncChargedStaffRiptidePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(SyncChargedStaffRiptidePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.setPacketHandled(true);
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private static void handle(SyncChargedStaffRiptidePacket packet) {
            var level = Minecraft.getInstance().level;
            if (level != null && level.getEntity(packet.entityId) instanceof Player player) {
                ChargedTwinBladeStaffRiptide.acceptSync(player, packet.active, packet.maintenanceInput);
            }
        }
    }
}
