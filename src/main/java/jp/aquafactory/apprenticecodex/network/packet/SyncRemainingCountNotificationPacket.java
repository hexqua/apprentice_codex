package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletClientNotificationState;
import jp.aquafactory.apprenticecodex.utility.CompactCountFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncRemainingCountNotificationPacket(
        String sourceId,
        ItemStack iconStack,
        long remainingCount,
        DisplayType displayType
) {
    public static void encode(SyncRemainingCountNotificationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourceId);
        buffer.writeItem(packet.iconStack);
        buffer.writeVarLong(packet.remainingCount);
        buffer.writeEnum(packet.displayType);
    }

    public static SyncRemainingCountNotificationPacket decode(FriendlyByteBuf buffer) {
        return new SyncRemainingCountNotificationPacket(
                buffer.readUtf(),
                buffer.readItem(),
                buffer.readVarLong(),
                buffer.readEnum(DisplayType.class)
        );
    }

    public static void handle(SyncRemainingCountNotificationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
    }

    public enum DisplayType {
        ITEM_REMAINING,
        MANA_REMAINING;

        public String format(long count) {
            var normalizedCount = Math.max(0L, count);
            return switch (this) {
                case ITEM_REMAINING -> CompactCountFormatter.format(normalizedCount);
                case MANA_REMAINING -> Long.toString(normalizedCount);
            };
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncRemainingCountNotificationPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            var sourceId = ResourceLocation.tryParse(packet.sourceId);
            if (sourceId == null || packet.iconStack.isEmpty()) {
                return;
            }

            AutocastAmuletClientNotificationState.updateRemainingCount(
                    sourceId,
                    packet.iconStack,
                    packet.displayType.format(packet.remainingCount),
                    packet.displayType
            );
        }
    }
}
