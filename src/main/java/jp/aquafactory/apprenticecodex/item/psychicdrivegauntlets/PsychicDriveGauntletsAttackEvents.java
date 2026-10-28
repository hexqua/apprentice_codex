package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.Input;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsAttackState.UseAction;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.PsychicDriveGauntletsEffectPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncPsychicDriveGauntletsPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncPsychicDriveGauntletsUseStatePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ApprenticeCodex.MODID)
public final class PsychicDriveGauntletsAttackEvents {
    private record Entry(Player player, PsychicDriveGauntletsAttackState state) {}
    private static final Map<UUID, Entry> STATES = new HashMap<>();
    private static final Map<UUID, Mode> MODES = new HashMap<>();
    private PsychicDriveGauntletsAttackEvents() {}

    public static void input(ServerPlayer player, Input input) {
        if (input == Input.RELEASE) {
            var entry = STATES.get(player.getUUID());
            if (entry != null && entry.player == player) entry.state.input(player, input, player.level().getGameTime());
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

    public static void useInput(ServerPlayer player, UseAction action, InteractionHand hand, long session) {
        if (session <= 0) return;
        var entry = STATES.compute(player.getUUID(), (id, previous) ->
                previous != null && previous.player == player ? previous
                        : new Entry(player, new PsychicDriveGauntletsAttackState()));
        if (action == UseAction.START && !entry.state.usePressed()
                && PsychicDriveGauntletsSelection.isGauntlets(player.getItemInHand(hand))
                && PsychicDriveGauntletsSelection.duplicate(player)) duplicateError(player);
        boolean accepted = entry.state.useInput(player, action, hand, session, player.level().getGameTime());
        PsychicDriveGauntletsChargeMovement.update(player, entry.state.charging());
        if (!(player instanceof FakePlayer)) {
            Networks.sendToPlayer(player, new SyncPsychicDriveGauntletsUseStatePacket(session, accepted));
        }
        sync(player, entry.state);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
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
        if (entry == null || entry.player != player) {
            PsychicDriveGauntletsChargeMovement.update(player, false);
            return;
        }
        boolean wasActive = entry.state.active();
        boolean wasCharging = entry.state.charging();
        entry.state.tick(player, player.level().getGameTime());
        PsychicDriveGauntletsChargeMovement.update(player, entry.state.charging());
        if (wasCharging && !entry.state.charging() && !(player instanceof FakePlayer)) {
            Networks.sendToPlayer(player, new SyncPsychicDriveGauntletsUseStatePacket(entry.state.useSession(), false));
        }
        if (wasActive || entry.state.active() || MODES.get(player.getUUID()) != mode) sync(player, entry.state);
        if (!entry.state.active() && PsychicDriveGauntletsSelection.find(player, true).mode()
                == Mode.HIDDEN && entry.state.useSession() == 0) STATES.remove(player.getUUID());
    }

    private static void sync(ServerPlayer player, PsychicDriveGauntletsAttackState state) {
        MODES.put(player.getUUID(), PsychicDriveGauntletsSelection.find(player, true).mode());
        if (!(player instanceof FakePlayer)) Networks.sendToTrackingEntityAndSelf(player, packet(player, state));
        dispatchEffects(player, state.right());
        dispatchEffects(player, state.left());
    }

    private static void dispatchEffects(ServerPlayer player, PsychicDriveGauntletsArm arm) {
        for (var effect : arm.drainEffects()) {
            if (player instanceof FakePlayer) continue;
            if (effect.kind() == PsychicDriveGauntletsEffect.Kind.CHARGE_READY) {
                var sound = effect.sound();
                if (sound != null) player.playNotifySound(sound, SoundSource.PLAYERS, 1, 1);
            }
            // 最大到達の光は周囲にも見せるが、通知音は使用者だけへ送る。
            Networks.sendToTrackingEntityAndSelf(player, new PsychicDriveGauntletsEffectPacket(player.getUUID(), effect));
        }
    }

    private static SyncPsychicDriveGauntletsPacket packet(Player player, PsychicDriveGauntletsAttackState state) {
        return new SyncPsychicDriveGauntletsPacket(player.getUUID(), false, state.right().snapshot(), state.left().snapshot(),
                PsychicDriveGauntletsSelection.find(player, true).mode());
    }

    private static void clear(Player player) {
        MODES.remove(player.getUUID());
        var entry = STATES.remove(player.getUUID());
        if (entry != null) {
            entry.state.right().reset();
            entry.state.left().reset();
        }
        PsychicDriveGauntletsChargeMovement.update(player, false);
        if (entry != null && player instanceof ServerPlayer server && !(server instanceof FakePlayer)) {
            Networks.sendToPlayer(server, new SyncPsychicDriveGauntletsUseStatePacket(entry.state.useSession(), false));
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
    // 保存後のStoppedでは拾得禁止状態が残るため、保存前に追従を解除する。
    @SubscribeEvent public static void stop(ServerStoppingEvent event) {
        for (var entry : STATES.values()) {
            entry.state.right().reset();
            entry.state.left().reset();
        }
        STATES.clear();
        MODES.clear();
    }
}
