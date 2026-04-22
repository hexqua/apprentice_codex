package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.item.curios.autocastamulet.AutocastAmuletClientNotificationState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncAutocastAmuletNotificationPacket(NotificationType type, String spellId, int cooldownTicks) {
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

    public static void handle(SyncAutocastAmuletNotificationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
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
