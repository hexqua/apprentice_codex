package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.Input;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncPsychicDriveGauntletsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PsychicDriveGauntletsAttackEvents {
    private record Entry(Player player, PsychicDriveGauntletsAttackState state) {}
    private static final Map<UUID, Entry> STATES = new HashMap<>();
    private static final Map<UUID, Mode> MODES = new HashMap<>();
    private PsychicDriveGauntletsAttackEvents() {}

    public static void input(ServerPlayer player, Input input) {
        if (input == Input.RELEASE || input == Input.CANCEL_USE || input == Input.RESET_USE) {
            var entry = STATES.get(player.getUUID());
            if (entry != null && entry.player == player) entry.state.input(player, input, player.level().getGameTime());
            if (input == Input.CANCEL_USE) {
                if (PsychicDriveGauntletsSelection.isGauntlets(player.getUseItem())) player.stopUsingItem();
                if (entry != null && entry.player == player) sync(player, entry.state);
            }
            return;
        }
        if (!player.isAlive() || player.isSpectator()
                || !PsychicDriveGauntletsSelection.isGauntlets(player.getMainHandItem())) return;
        if (PsychicDriveGauntletsSelection.duplicate(player)) {
            if (input == Input.PRESS) duplicateError(player);
            return;
        }
        var entry = STATES.compute(player.getUUID(), (id, previous) ->
                previous != null && previous.player == player ? previous
                        : new Entry(player, new PsychicDriveGauntletsAttackState()));
        if (entry.state.input(player, input, player.level().getGameTime())) sync(player, entry.state);
    }

    public static void duplicateError(Player player) {
        player.displayClientMessage(Component.translatable("ui.apprenticecodex.psychic_drive_gauntlets.duplicate_hotbar")
                .withStyle(ChatFormatting.RED), true);
    }

    public static boolean beginUse(Player player, InteractionHand hand) {
        var entry = STATES.compute(player.getUUID(), (id, previous) ->
                previous != null && previous.player == player ? previous
                        : new Entry(player, new PsychicDriveGauntletsAttackState()));
        if (!entry.state.usePressed() && PsychicDriveGauntletsSelection.duplicate(player)) duplicateError(player);
        boolean accepted = entry.state.beginUse(player, hand, player.level().getGameTime());
        if (player instanceof ServerPlayer server && !(server instanceof FakePlayer)) {
            var packet = packet(player, entry.state);
            Networks.sendToTrackingEntityAndSelf(server, new SyncPsychicDriveGauntletsPacket(packet.player(), packet.reset(),
                    packet.right(), packet.left(), packet.mode(), !accepted));
        }
        return accepted;
    }

    public static void releaseUse(Player player) {
        var entry = STATES.get(player.getUUID());
        if (entry == null || entry.player != player) return;
        entry.state.releaseUse(player, player.level().getGameTime());
        if (player instanceof ServerPlayer server) sync(server, entry.state);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isAlive() || player.isSpectator()) {
            clear(player);
            return;
        }
        var entry = STATES.get(player.getUUID());
        var mode = PsychicDriveGauntletsSelection.find(player, true).mode();
        if (entry == null && mode != Mode.HIDDEN) {
            entry = new Entry(player, new PsychicDriveGauntletsAttackState());
            STATES.put(player.getUUID(), entry);
        }
        if (entry == null || entry.player != player) return;
        boolean wasActive = entry.state.active();
        if (entry.state.charging() && !player.isUsingItem()) entry.state.cancelUse();
        entry.state.tick(player, player.level().getGameTime());
        if (wasActive || entry.state.active() || MODES.get(player.getUUID()) != mode) sync(player, entry.state);
        if (!entry.state.active() && PsychicDriveGauntletsSelection.find(player, true).mode()
                == Mode.HIDDEN) STATES.remove(player.getUUID());
    }

    private static void sync(ServerPlayer player, PsychicDriveGauntletsAttackState state) {
        MODES.put(player.getUUID(), PsychicDriveGauntletsSelection.find(player, true).mode());
        if (!(player instanceof FakePlayer)) Networks.sendToTrackingEntityAndSelf(player, packet(player, state));
    }

    private static SyncPsychicDriveGauntletsPacket packet(Player player, PsychicDriveGauntletsAttackState state) {
        return new SyncPsychicDriveGauntletsPacket(player.getUUID(), false, state.right().snapshot(), state.left().snapshot(),
                PsychicDriveGauntletsSelection.find(player, true).mode());
    }

    private static void clear(Player player) {
        MODES.remove(player.getUUID());
        var entry = STATES.remove(player.getUUID());
        if (entry != null && player instanceof ServerPlayer server && !(server instanceof FakePlayer)) {
            var idle = new PsychicDriveGauntletsAttackState();
            Networks.sendToTrackingEntityAndSelf(server, new SyncPsychicDriveGauntletsPacket(player.getUUID(),
                    true, idle.right().snapshot(), idle.left().snapshot()));
        }
    }

    @SubscribeEvent public static void track(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && !(viewer instanceof FakePlayer)
                && event.getTarget() instanceof Player target) {
            var entry = STATES.get(target.getUUID());
            if (entry != null) Networks.sendToPlayer(viewer, packet(target, entry.state));
        }
    }
    @SubscribeEvent public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (PsychicDriveGauntletsSelection.isGauntlets(event.getEntity().getMainHandItem())) event.setCanceled(true);
    }
    @SubscribeEvent public static void attackEntity(AttackEntityEvent event) {
        if (PsychicDriveGauntletsSelection.isGauntlets(event.getEntity().getMainHandItem())) event.setCanceled(true);
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) clear(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { clear(event.getEntity()); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { clear(event.getEntity()); }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) { clear(event.getOriginal()); }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { STATES.clear(); MODES.clear(); }
}
