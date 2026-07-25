package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletClientNotificationState;
import jp.aquafactory.apprenticecodex.utility.CompactCountFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncRemainingCountNotificationPacket(
        String sourceId,
        ItemStack iconStack,
        long remainingCount,
        DisplayType displayType
) implements CustomPacketPayload {
    public static final Type<SyncRemainingCountNotificationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_remaining_count_notification"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRemainingCountNotificationPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncRemainingCountNotificationPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void encode(SyncRemainingCountNotificationPacket packet, RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(packet.sourceId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, packet.iconStack);
        buffer.writeVarLong(packet.remainingCount);
        buffer.writeEnum(packet.displayType);
    }

    public static SyncRemainingCountNotificationPacket decode(RegistryFriendlyByteBuf buffer) {
        return new SyncRemainingCountNotificationPacket(
                buffer.readUtf(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                buffer.readVarLong(),
                buffer.readEnum(DisplayType.class)
        );
    }

    public static void handle(SyncRemainingCountNotificationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
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
