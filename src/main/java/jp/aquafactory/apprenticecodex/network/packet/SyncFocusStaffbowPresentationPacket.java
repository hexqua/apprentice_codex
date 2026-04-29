package jp.aquafactory.apprenticecodex.network.packet;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.render.animation.AnimationHelper;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbowClientPresentationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SyncFocusStaffbowPresentationPacket(UUID entityId, String spellId, PresentationAction action, CompoundTag data) {
    public SyncFocusStaffbowPresentationPacket(UUID entityId, String spellId, PresentationAction action) {
        this(entityId, spellId, action, new CompoundTag());
    }

    public SyncFocusStaffbowPresentationPacket {
        data = data == null ? new CompoundTag() : data.copy();
    }

    public static void encode(SyncFocusStaffbowPresentationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.entityId);
        buffer.writeUtf(packet.spellId);
        buffer.writeEnum(packet.action);
        buffer.writeNbt(packet.data);
    }

    public static SyncFocusStaffbowPresentationPacket decode(FriendlyByteBuf buffer) {
        return new SyncFocusStaffbowPresentationPacket(
                buffer.readUUID(),
                buffer.readUtf(),
                buffer.readEnum(PresentationAction.class),
                buffer.readNbt()
        );
    }

    public static void handle(SyncFocusStaffbowPresentationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet))
        );
        context.setPacketHandled(true);
    }

    public enum PresentationAction {
        START_PENDING,
        CANCEL_PENDING
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void handle(SyncFocusStaffbowPresentationPacket packet) {
            if (packet.action == PresentationAction.START_PENDING) {
                handleStart(packet);
                return;
            }

            FocusStaffbowClientPresentationState.clear(packet.entityId);
            cancelPlayerAnimation(packet.entityId);
        }

        private static void handleStart(SyncFocusStaffbowPresentationPacket packet) {
            FocusStaffbowClientPresentationState.markPending(packet.entityId, packet.spellId, packet.data);

            var player = resolvePlayer(packet.entityId);
            if (player == null) {
                return;
            }

            SpellAnimations.BOW_CHARGE_ANIMATION.getForPlayer()
                    .ifPresent(animation -> AnimationHelper.animatePlayerStart(player, animation));

            var spell = SpellRegistry.getSpell(packet.spellId);
            if (spell != null && spell != SpellRegistry.none()) {
                spell.playSound(spell.getCastStartSound(), player);
            }
        }

        private static void cancelPlayerAnimation(UUID entityId) {
            var player = resolvePlayer(entityId);
            if (player instanceof AbstractClientPlayer clientPlayer) {
                AnimationHelper.cancelPlayerAnimation(clientPlayer);
            }
        }

        private static Player resolvePlayer(UUID entityId) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return null;
            }

            return minecraft.level.getPlayerByUUID(entityId);
        }
    }
}
