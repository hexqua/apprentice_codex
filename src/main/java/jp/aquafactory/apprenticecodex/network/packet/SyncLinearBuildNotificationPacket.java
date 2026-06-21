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

public record SyncLinearBuildNotificationPacket(String spellId, ItemStack iconStack, long remainingBlocks) {
    public static void encode(SyncLinearBuildNotificationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.spellId);
        buffer.writeItem(packet.iconStack);
        buffer.writeVarLong(packet.remainingBlocks);
    }

    public static SyncLinearBuildNotificationPacket decode(FriendlyByteBuf buffer) {
        return new SyncLinearBuildNotificationPacket(
                buffer.readUtf(),
                buffer.readItem(),
                buffer.readVarLong()
        );
    }

    public static void handle(SyncLinearBuildNotificationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

        private static void handle(SyncLinearBuildNotificationPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            var spellId = ResourceLocation.tryParse(packet.spellId);
            if (spellId == null || packet.iconStack.isEmpty()) {
                return;
            }

            AutocastAmuletClientNotificationState.queueLinearBuildRemaining(
                    spellId,
                    packet.iconStack,
                    CompactCountFormatter.format(packet.remainingBlocks)
            );
        }
    }
}
