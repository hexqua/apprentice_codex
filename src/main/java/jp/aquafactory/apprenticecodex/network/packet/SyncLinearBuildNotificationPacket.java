package jp.aquafactory.apprenticecodex.network.packet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletClientNotificationState;
import jp.aquafactory.apprenticecodex.utility.CompactCountFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
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

public record SyncLinearBuildNotificationPacket(String spellId, ItemStack iconStack, long remainingBlocks)
        implements CustomPacketPayload {
    public static final Type<SyncLinearBuildNotificationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_linear_build_notification"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLinearBuildNotificationPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncLinearBuildNotificationPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncLinearBuildNotificationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.spellId);
        ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, packet.iconStack);
        buffer.writeVarLong(packet.remainingBlocks);
    }

    public static SyncLinearBuildNotificationPacket decode(FriendlyByteBuf buffer) {
        return new SyncLinearBuildNotificationPacket(
                buffer.readUtf(),
                ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer),
                buffer.readVarLong()
        );
    }

    public static void handle(SyncLinearBuildNotificationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
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

            AutocastAmuletClientNotificationState.updateLinearBuildRemaining(
                    spellId,
                    packet.iconStack,
                    CompactCountFormatter.format(packet.remainingBlocks)
            );
        }
    }
}
