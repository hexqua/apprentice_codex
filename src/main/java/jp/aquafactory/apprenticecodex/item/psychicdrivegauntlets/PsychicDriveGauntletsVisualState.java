package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Impact;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class PsychicDriveGauntletsVisualState {
    public static final float NORMAL_RING_START = 0.3F;
    public static final float NORMAL_RING_END = 0.7F;
    public static final int NORMAL_RING_TICKS = 5;
    public static final float NORMAL_RING_STRENGTH = 0.55F;
    public static final float CONTACT_RING_START = 0.55F;
    public static final float CONTACT_RING_END = 0.65F;
    public static final int CONTACT_RING_TICKS = 3;
    public static final float CONTACT_RING_STRENGTH = 0.7F;
    public static final double AURA_FADE_TICKS = 2;
    public static final float AURA_MIN_STRENGTH = 0.025F;
    public static final float AURA_CHARGE_STRENGTH = 0.055F;
    public static final float AURA_SCALE = 1.035F;
    public static final int INITIAL_SPARKS = 6;
    public static final int INTERMEDIATE_SPARKS = 3;
    public static final int CONTINUOUS_SPARKS = 2;
    public static final float SPARK_RED = 0.72F;
    public static final float SPARK_GREEN = 0.26F;
    public static final float SPARK_BLUE = 1;
    public static final int SPARK_MIN_LIFETIME = 10;
    public static final int SPARK_LIFETIME_VARIANCE = 4;
    public static final int SPARK_BOUNCE_TICKS = 4;
    public static final float SPARK_GRAVITY = 1;
    public static final float SPARK_FRICTION = 0.96F;
    public static final float SPARK_BOUNCE_FACTOR = 0.25F;
    public static final float SPARK_MIN_SIZE = 0.04F;
    public static final float SPARK_MAX_SIZE = 0.06F;
    public static final double SPARK_MIN_SIDE_SPEED = 0.10;
    public static final double SPARK_MAX_SIDE_SPEED = 0.18;
    public static final double SPARK_MIN_UP_SPEED = 0.10;
    public static final double SPARK_MAX_UP_SPEED = 0.16;

    private float lastAura;
    private boolean powered;
    private double fadeStarted;
    private long drillLaunch = Long.MIN_VALUE;
    private long drillImpact = Long.MIN_VALUE;
    private long lastDrillTick = Long.MIN_VALUE;

    public float aura(@Nullable Snapshot arm, double time) {
        // 命中後も連続ダメージ中は光膜を保ち、終了・中断で帰還してからフェードする。
        boolean active = arm != null && (arm.phase() == Phase.CHARGING
                || arm.phase() == Phase.OUTBOUND && arm.charge() > 0 || drilling(arm));
        if (active) {
            powered = true;
            lastAura = AURA_MIN_STRENGTH + Mth.clamp(arm.charge(), 0, 1) * AURA_CHARGE_STRENGTH;
            return lastAura;
        }
        if (powered) {
            powered = false;
            fadeStarted = time;
        }
        // 同じ終端snapshotを何度描いてもフェード開始時刻を更新しない。
        return lastAura * (float) Mth.clamp(1 - (time - fadeStarted) / AURA_FADE_TICKS, 0, 1);
    }

    public static boolean drilling(@Nullable Snapshot arm) {
        return arm != null && arm.phase() == Phase.HOLDING && arm.charge() >= 1
                && arm.impact() == Impact.ENTITY && arm.holdTicks() == PsychicDriveGauntletsArm.DRILL_TICKS;
    }

    public boolean emitContinuousSpark(@Nullable Snapshot arm, double time) {
        if (!drilling(arm)) {
            drillLaunch = Long.MIN_VALUE;
            drillImpact = Long.MIN_VALUE;
            lastDrillTick = Long.MIN_VALUE;
            return false;
        }
        if (drillLaunch != arm.launchTick() || drillImpact != arm.impactTick()) {
            drillLaunch = arm.launchTick();
            drillImpact = arm.impactTick();
            lastDrillTick = Long.MIN_VALUE;
        }
        long elapsed = (long) Math.floor(time - arm.motion().phaseStartedTick());
        if (elapsed <= lastDrillTick) return false;
        lastDrillTick = elapsed;
        // 4・8tickの成功段は既存イベントで放出する。遅延時も過去の分は補充しない。
        return elapsed == 2 || elapsed == 6 || elapsed == 10;
    }
}
