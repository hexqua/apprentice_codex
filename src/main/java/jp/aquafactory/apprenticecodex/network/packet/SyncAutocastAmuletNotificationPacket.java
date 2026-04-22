package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletClientNotificationState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SyncAutocastAmuletNotificationPacket(NotificationType type, String spellId, int cooldownTicks)
        implements CustomPacketPayload {
    public static final Type<SyncAutocastAmuletNotificationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ApprenticeCodex.MODID, "sync_autocast_amulet_notification"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAutocastAmuletNotificationPacket> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> encode(packet, buffer), SyncAutocastAmuletNotificationPacket::decode);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncAutocastAmuletNotificationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.type);
        buffer.writeUtf(packet.spellId);
        buffer.writeVarInt(packet.cooldownTicks);
    }

    public static SyncAutocastAmuletNotificationPacket decode(FriendlyByteBuf buffer) {
        return new SyncAutocastAmuletNotificationPacket(
                buffer.readEnum(NotificationType.class),
                buffer.readUtf(),
                buffer.readVarInt()
        );
    }

    public static void handle(SyncAutocastAmuletNotificationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientHandler.handle(packet);
            }
        });
    }

    public enum NotificationType {
        COOLDOWN_CAST,
        MANA_LOW
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncAutocastAmuletNotificationPacket packet) {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            var spell = SpellRegistry.getSpell(packet.spellId);
            if (spell == null || spell == SpellRegistry.none()) {
                return;
            }

            if (packet.type == NotificationType.MANA_LOW) {
                AutocastAmuletClientNotificationState.queueManaLow(spell.getSpellResource(), spell.getSpellIconResource());
                return;
            }

            AutocastAmuletClientNotificationState.queueCooldownCast(
                    spell.getSpellResource(),
                    spell.getSpellIconResource(),
                    packet.cooldownTicks
            );
        }
    }
}
