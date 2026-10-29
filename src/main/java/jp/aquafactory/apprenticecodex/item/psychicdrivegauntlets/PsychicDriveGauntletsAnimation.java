package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Impact;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Phase;
import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsArm.Snapshot;
import org.jetbrains.annotations.Nullable;

public final class PsychicDriveGauntletsAnimation {
    // 帰還移動の5tickとは独立し、モデルの0.45秒のrecoverを最後まで再生する。
    public static final double RECOVER_TICKS = 9.0;
    public enum Stage { IDLE, WINDUP, STRIKE, RECOVER }

    private Stage stage = Stage.IDLE;
    private long launchTick = Long.MIN_VALUE;
    private long revision;
    private boolean returning;
    private double recoverStarted;

    public Stage stage() { return stage; }
    public long revision() { return revision; }

    public void update(@Nullable Snapshot arm, double time) {
        if (arm != null && arm.phase() != Phase.IDLE && arm.phase() != Phase.CHARGING) {
            if (launchTick != arm.launchTick()) {
                launchTick = arm.launchTick();
                returning = false;
                // 同じ種類のアニメーションへ続けて切り替わっても、新しい攻撃は先頭から再生する。
                revision++;
            }
            if (arm.phase() == Phase.RETURNING) {
                if (!returning) {
                    returning = true;
                    recoverStarted = time;
                }
                setStage(time - recoverStarted < RECOVER_TICKS ? Stage.RECOVER : Stage.IDLE);
            } else {
                setStage(arm.charge() >= 1 && (arm.impact() == Impact.ENTITY || arm.phase() == Phase.MINING)
                        ? Stage.STRIKE : Stage.WINDUP);
            }
        } else if (arm != null && arm.phase() == Phase.CHARGING) {
            setStage(Stage.IDLE);
        } else if (stage != Stage.RECOVER || time - recoverStarted >= RECOVER_TICKS) {
            setStage(Stage.IDLE);
        }
    }

    private void setStage(Stage next) {
        if (stage != next) {
            stage = next;
            revision++;
        }
    }
}
