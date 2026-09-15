package jp.aquafactory.apprenticecodex.event.client;

import jp.aquafactory.apprenticecodex.network.Networks;
import jp.aquafactory.apprenticecodex.network.packet.ClientInterruptCartridgeReloadPacket;
import jp.aquafactory.apprenticecodex.network.packet.SyncQuickcastCartridgePacket;

public final class QuickcastCartridgeClientState {
    private static SyncQuickcastCartridgePacket state;
    private static long ticks;
    private static long receivedAt;
    private static long blinkUntil;
    private static boolean interrupted;
    private QuickcastCartridgeClientState() {}

    public static void accept(SyncQuickcastCartridgePacket packet) {
        boolean wasEquipped = state != null && state.equipped();
        state = packet;
        receivedAt = ticks;
        interrupted = false;
        if (packet.completed() && wasEquipped && packet.equipped()) blinkUntil = ticks + 20;
        if (!packet.equipped() || packet.reloadUntil() > 0) blinkUntil = 0;
    }

    public static void tick() { ticks++; }
    public static boolean equipped() { return state != null && state.equipped(); }
    public static boolean available() { return state != null && state.available(); }
    public static boolean reloading() { return state != null && state.reloadUntil() > 0 && !interrupted; }
    public static boolean blink() { return !reloading() && blinkUntil > ticks && (blinkUntil - ticks) / 3 % 2 == 1; }

    public static int chargeSteps(float partialTick) {
        if (state == null) return 0;
        if (state.available()) return 20;
        long duration = state.recoveryDuration();
        long until = state.recoveryUntil();
        if (duration <= 0) return 0;
        double remaining = remaining(until, partialTick);
        // 補間だけでは満タンにせず、回復確定packetを待つ。
        return (int) Math.max(0, Math.min(19, Math.floor(20 * (1 - remaining / duration))));
    }

    public static double reloadRemainingTicks(float partialTick) {
        if (!reloading()) return 0;
        return Math.max(0, Math.min(state.reloadDuration(), remaining(state.reloadUntil(), partialTick)));
    }

    public static float reloadProgress(float partialTick) {
        if (!reloading() || state.reloadDuration() <= 0) return 0;
        return (float) (1 - reloadRemainingTicks(partialTick) / state.reloadDuration());
    }

    private static double remaining(long until, float partialTick) {
        return (double) until - state.serverTime() - (ticks - receivedAt) - partialTick;
    }

    public static void interrupt() {
        if (!reloading()) return;
        interrupted = true;
        Networks.sendToServer(new ClientInterruptCartridgeReloadPacket());
    }

    public static void reset() {
        state = null;
        blinkUntil = 0;
        interrupted = false;
    }
}
