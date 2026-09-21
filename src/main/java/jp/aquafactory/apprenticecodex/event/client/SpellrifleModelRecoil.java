package jp.aquafactory.apprenticecodex.event.client;

import java.util.ArrayList;
import java.util.List;

/** カメラ反動とは独立した、銃と腕の短い跳ね上がり。 */
final class SpellrifleModelRecoil {
    private static final double RISE_TICKS = 0.6;
    private static final double DURATION_TICKS = 3.6;
    private static final float MAX_AMOUNT = 1.6F;
    private final List<Impulse> impulses = new ArrayList<>();

    void clear() {
        impulses.clear();
    }

    void fire(double time, float strength) {
        amount(time);
        // 通常の連射間隔では到達しない上限。異常な通知密度でも履歴を増やし続けない。
        if (impulses.size() >= 16) {
            impulses.removeFirst();
        }
        impulses.add(new Impulse(time, strength));
    }

    float amount(double time) {
        impulses.removeIf(impulse -> time < impulse.time() || time - impulse.time() >= DURATION_TICKS);
        float total = 0;
        for (var impulse : impulses) {
            double age = time - impulse.time();
            double progress = age < RISE_TICKS ? age / RISE_TICKS
                    : 1.0 - (age - RISE_TICKS) / (DURATION_TICKS - RISE_TICKS);
            // 30msで立ち上がり、保持せず150msで戻す。連射は復帰途中へ小さな衝撃を重ねる。
            double weight = progress * progress * (3.0 - 2.0 * progress);
            total += (float) (weight * impulse.strength());
        }
        return Math.min(MAX_AMOUNT, total);
    }

    private record Impulse(double time, float strength) {
    }
}
