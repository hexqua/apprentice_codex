package jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets;

import jp.aquafactory.apprenticecodex.item.psychicdrivegauntlets.PsychicDriveGauntletsSelection.Mode;
import net.minecraft.util.Mth;

public final class PsychicDriveGauntletsTransition {
    private static final double DURATION_TICKS = 10.0;
    private Mode mode = Mode.HIDDEN;
    private double startTime;
    private float startAmount;
    private float targetAmount;

    public void update(Mode nextMode, double time) {
        if (nextMode == mode) {
            return;
        }
        var nextAmount = nextMode == Mode.DRAWN ? 1.0F : 0.0F;
        // 初出現は完成位置へ置き、反転時だけ現在の補間結果を出発点として保つ。
        startAmount = mode == Mode.HIDDEN || nextMode == Mode.HIDDEN ? nextAmount : amount(time);
        targetAmount = nextAmount;
        startTime = time;
        mode = nextMode;
    }

    public float amount(double time) {
        var progress = Mth.clamp((time - startTime) / DURATION_TICKS, 0.0, 1.0);
        var remaining = 1.0 - progress;
        var eased = (float) (1.0 - remaining * remaining * remaining);
        return Mth.lerp(eased, startAmount, targetAmount);
    }
}
