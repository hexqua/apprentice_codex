package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public final class PsychicDriveGauntletsHudState {
    public record Gauge(float recovery, float charge) {}
    public static final Gauge READY = new Gauge(1, 0);

    private PsychicDriveGauntletsHudState() {}

    public static boolean canDisplay(Player player) {
        // 左攻撃は非空のoffhandでも使える。溜めだけの装備判定でHUDを隠さない。
        return player.isAlive() && !player.isSpectator() && !PsychicDriveGauntletsSelection.duplicate(player)
                && (PsychicDriveGauntletsSelection.isGauntlets(player.getMainHandItem())
                || PsychicDriveGauntletsSelection.canUse(player, InteractionHand.OFF_HAND));
    }

    public static Gauge sample(Snapshot arm, double elapsedSinceSample) {
        double time = arm.sampleTick() + Math.max(0, elapsedSinceSample);
        double elapsed = Math.max(0, time - arm.recoveryStartedTick());
        return switch (arm.phase()) {
            case IDLE -> READY;
            case PREPARING, OUTBOUND -> new Gauge(0, 0);
            case CHARGING -> new Gauge(1, PsychicDriveGauntletsCharge.amount(time - arm.launchTick()));
            case HOLDING, MINING, FOLLOW_THROUGH -> new Gauge((float) Math.min(elapsed, arm.holdTicks())
                    / (arm.holdTicks() + PsychicDriveGauntletsArm.RETURN_TICKS), 0);
            // serverの待機状態を受信するまでは満タンにしない。
            case RETURNING -> new Gauge(Math.min(Math.nextDown(1.0F), Mth.lerp(
                    (float) Mth.clamp(elapsed / PsychicDriveGauntletsArm.RETURN_TICKS, 0, 1),
                    arm.returnRecovery(), 1)), 0);
        };
    }
}
