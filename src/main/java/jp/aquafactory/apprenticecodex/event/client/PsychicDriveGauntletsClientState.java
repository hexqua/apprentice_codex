package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsDisplayMotion;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsHudState;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsHudState.Gauge;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection;
import jp.aquafactory.apprenticecodex.network.packet.SyncPsychicDriveGauntletsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PsychicDriveGauntletsClientState {
    private record State(PsychicDriveGauntletsDisplayMotion right, PsychicDriveGauntletsDisplayMotion left,
                         long received) {}
    private record HudState(Snapshot right, Snapshot left, double receivedTime) {}
    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Map<UUID, HudState> HUD_STATES = new HashMap<>();
    private static final Map<UUID, Mode> MODES = new HashMap<>();
    private static ClientLevel level;
    private static double lastTime;
    private PsychicDriveGauntletsClientState() {}

    private static void reset() {
        var current = Minecraft.getInstance().level;
        if (current != level) {
            STATES.clear();
            HUD_STATES.clear();
            MODES.clear();
            level = current;
            lastTime = current == null ? 0.0 : current.getGameTime();
        }
    }
    public static void accept(SyncPsychicDriveGauntletsPacket packet) {
        reset();
        if (level == null) return;
        if (packet.reset()) {
            PsychicDriveGauntletsEffects.clear(packet.player());
            // 死亡・dimension移動は帰還完了と区別し、古い座標へ補間せず即座に破棄する。
            STATES.remove(packet.player());
            HUD_STATES.remove(packet.player());
            MODES.remove(packet.player());
            return;
        }
        MODES.put(packet.player(), packet.mode());
        if (packet.mode() == Mode.DISABLED) {
            PsychicDriveGauntletsEffects.clear(packet.player());
            STATES.remove(packet.player());
            HUD_STATES.remove(packet.player());
            return;
        }
        var time = clock(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        HUD_STATES.put(packet.player(), new HudState(packet.right(), packet.left(), time));
        var old = STATES.get(packet.player());
        if (packet.right().phase() == Phase.IDLE && packet.left().phase() == Phase.IDLE) {
            if (old == null) return;
        }
        STATES.put(packet.player(), new State(arm(old == null ? null : old.right, packet.right(), time),
                arm(old == null ? null : old.left, packet.left(), time), level.getGameTime()));
    }
    private static PsychicDriveGauntletsDisplayMotion arm(@Nullable PsychicDriveGauntletsDisplayMotion old,
                                                          Snapshot current, double time) {
        if (old == null) return new PsychicDriveGauntletsDisplayMotion(current, time);
        old.accept(current, time);
        return old;
    }
    private static double clock(float partialTick) {
        // fraction単独ではtick境界で巻き戻る。受信・描画とも同じ連続したclient時刻を使う。
        lastTime = Math.max(lastTime, level.getGameTime() + (double) partialTick);
        return lastTime;
    }
    public static void prune() {
        reset();
        if (level == null) return;
        MODES.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        // 描画用の期限切れを待機完了と誤認しないよう、HUDは明示同期まで最後の状態を保持する。
        HUD_STATES.keySet().removeIf(id -> level.getPlayerByUUID(id) == null);
        var time = clock(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        STATES.entrySet().removeIf(entry -> level.getPlayerByUUID(entry.getKey()) == null
                || level.getGameTime() - entry.getValue().received > 20
                || (entry.getValue().right.finished(time) && entry.getValue().left.finished(time)));
    }

    public static Mode mode(Player player) {
        reset();
        // 自分はinventory変更を即座に反映し、他人の非選択ホットバーはserverの判定を使う。
        if (player == Minecraft.getInstance().player) {
            var mode = PsychicDriveGauntletsSelection.find(player, true).mode();
            if (mode == Mode.DISABLED) {
                STATES.remove(player.getUUID());
                HUD_STATES.remove(player.getUUID());
            }
            return mode;
        }
        return MODES.getOrDefault(player.getUUID(), PsychicDriveGauntletsSelection.find(player, false).mode());
    }
    public static @Nullable Snapshot get(UUID player, boolean right, float partialTick) {
        reset();
        var state = STATES.get(player);
        if (state == null || level == null) return null;
        if (level.getGameTime() - state.received > 20 || level.getPlayerByUUID(player) == null) {
            STATES.remove(player);
            return null;
        }
        var arm = right ? state.right : state.left;
        var displayed = arm.sample(clock(partialTick));
        return displayed.phase() == Phase.IDLE ? null : displayed;
    }
    public static boolean active(UUID player) {
        var partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        return get(player, true, partialTick) != null || get(player, false, partialTick) != null;
    }

    public static Gauge hud(UUID player, boolean right, float partialTick) {
        reset();
        var state = HUD_STATES.get(player);
        if (state == null || level == null) {
            return PsychicDriveGauntletsHudState.READY;
        }
        // 3D側は帰還完了も一tick補間するが、HUDは最新のserver状態へ即座に追従する。
        return PsychicDriveGauntletsHudState.sample(right ? state.right : state.left,
                clock(partialTick) - state.receivedTime);
    }
}
