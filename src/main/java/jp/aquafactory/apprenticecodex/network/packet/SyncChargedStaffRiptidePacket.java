package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.chargedtwinbladestaff.ChargedTwinBladeStaffRiptide;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncChargedStaffRiptidePacket(int entityId, boolean active, boolean maintenanceInput) implements CustomPacketPayload {
    public static final Type<SyncChargedStaffRiptidePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_charged_staff_riptide"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncChargedStaffRiptidePacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> {
                buffer.writeVarInt(packet.entityId);
                buffer.writeBoolean(packet.active);
                buffer.writeBoolean(packet.maintenanceInput);
            },
            buffer -> new SyncChargedStaffRiptidePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean()));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncChargedStaffRiptidePacket packet, IPayloadContext context) {
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
