package jp.aquafactory.apprenticecodex.item.fullautorapidcastspellrifle;

import java.util.ArrayList;
import java.util.List;

public final class FullautoRapidcastSpellrifleRecoil {
    private static final double IMPULSE_DURATION_NANOS = 100_000_000.0;
    private static final float BASE_PITCH = 1.5F;
    private static final float ADS_MULTIPLIER = 0.45F;
    private static final float BUILDUP_PER_SHOT = 0.2F;
    private static final long RECOVERY_DELAY_TICKS = 4;
    private static final float RECOVERY_TICKS = 12.0F;

    private long lastShotTick = Long.MIN_VALUE;
    private float buildup;
    private final List<Impulse> impulses = new ArrayList<>();

    public void addImpulse(float pitch, long nowNanos) {
        impulses.add(new Impulse(pitch, nowNanos));
    }

    public float advanceImpulses(long nowNanos) {
        var delta = 0.0F;
        var iterator = impulses.iterator();
        while (iterator.hasNext()) {
            var impulse = iterator.next();
            var progress = Math.max(0.0, Math.min(1.0, (nowNanos - impulse.startNanos) / IMPULSE_DURATION_NANOS));
            // 発射直後に強く跳ね、終端で減速する quadratic ease-out。逆方向には戻さず総角度を保つ。
            var applied = (float) (impulse.pitch * progress * (2.0 - progress));
            delta += applied - impulse.applied;
            impulse.applied = applied;
            if (progress >= 1.0) iterator.remove();
        }
        return delta;
    }

    public void clearImpulses() {
        impulses.clear();
    }

    private static final class Impulse {
        private final float pitch;
        private final long startNanos;
        private float applied;

        private Impulse(float pitch, long startNanos) {
            this.pitch = pitch;
            this.startNanos = startNanos;
        }
    }

    public float fire(long tick, boolean ads) {
        if (lastShotTick == Long.MIN_VALUE || tick < lastShotTick) {
            buildup = 0.0F;
        } else {
            var recovery = Math.max(0L, tick - lastShotTick - RECOVERY_DELAY_TICKS) / RECOVERY_TICKS;
            buildup = Math.max(0.0F, buildup - recovery);
        }
        var multiplier = ads ? ADS_MULTIPLIER : 1.0F;
        var pitch = BASE_PITCH * (1.0F + buildup) * multiplier;
        buildup = Math.min(1.0F, buildup + BUILDUP_PER_SHOT * multiplier);
        lastShotTick = tick;
        return pitch;
    }
}
