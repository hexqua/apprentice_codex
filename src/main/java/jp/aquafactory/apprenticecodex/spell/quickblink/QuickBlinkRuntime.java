package jp.aquafactory.apprenticecodex.spell.quickblink;

import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleBlink;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleMovement;
import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.SyncQuickBlinkPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class QuickBlinkRuntime {
    private static final Map<UUID, State> SERVER = new HashMap<>();
    private static final Map<UUID, State> CLIENT = new HashMap<>();

    public static final class State {
        public final MantleBlink blink = new MantleBlink();
        private float forward;
        private float strafe;
        private long inputTime = Long.MIN_VALUE;
        private long sequence;
    }

    private QuickBlinkRuntime() { }

    public static State state(Player player) {
        return (player.level().isClientSide ? CLIENT : SERVER).computeIfAbsent(player.getUUID(), key -> new State());
    }

    public static void input(ServerPlayer player, float forward, float strafe) {
        if (!Float.isFinite(forward) || !Float.isFinite(strafe)
                || Math.abs(forward) > 1 || Math.abs(strafe) > 1) return;
        var state = state(player);
        state.forward = forward;
        state.strafe = strafe;
        state.inputTime = player.level().getGameTime();
    }

    public static boolean active(Player player) {
        return state(player).blink.active(player.level().getGameTime());
    }

    public static boolean cancelIncomingDamageIfInvulnerable(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && active(player)) {
            event.setCanceled(true);
            return true;
        }
        return false;
    }

    public static void begin(ServerPlayer player, double distance) {
        var state = state(player);
        long time = player.level().getGameTime();
        boolean fresh = state.inputTime != Long.MIN_VALUE && state.inputTime <= time && time - state.inputTime <= 10;
        Vec3 direction = MantleMovement.direction(fresh ? state.forward : 0, fresh ? state.strafe : 0, player.getYRot());
        state.blink.begin(player, ++state.sequence, direction, distance / 4);
        Networks.sendToTrackingEntityAndSelf(player, packet(player));
    }

    public static SyncQuickBlinkPacket packet(ServerPlayer player) {
        var blink = state(player).blink;
        return new SyncQuickBlinkPacket(player.getId(), blink.start(), blink.sequence(), blink.direction());
    }

    public static void accept(Player player, SyncQuickBlinkPacket packet) {
        var blink = state(player).blink;
        if (packet.start() < 0) blink.cancel();
        else blink.accept(packet.start(), packet.sequence(), packet.direction(), 5.0 / 4);
    }

    public static void tick(ServerPlayer player) {
        state(player).blink.update(player);
    }

    public static void clear(ServerPlayer player) {
        var state = SERVER.remove(player.getUUID());
        if (state != null && state.blink.start() >= 0) {
            state.blink.cancel();
            Networks.sendToTrackingEntityAndSelf(player,
                    new SyncQuickBlinkPacket(player.getId(), -1, state.sequence, Vec3.ZERO));
        }
    }

    public static void clearServer() { SERVER.clear(); }
    public static void clearClient() { CLIENT.clear(); }
}
