package jp.aquafactory.apprenticecodex.item.continuouscast;

public final class ContinuousCastDurationSimulation {
    private ContinuousCastDurationSimulation() {
    }

    public static int normalizeCastDuration(int castDuration) {
        return Math.max(1, castDuration);
    }

    public static int computeRemaining(int castDuration, long elapsedTicks) {
        var normalizedDuration = normalizeCastDuration(castDuration);
        var saturatedElapsedTicks = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, elapsedTicks));
        // 独自管理する無限 CONTINUOUS では0で止めず、魔法側の剰余スケジューラが一定周期で進むよう負数まで減らす。
        return normalizedDuration - saturatedElapsedTicks;
    }
}
