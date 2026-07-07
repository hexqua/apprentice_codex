package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;

public final class PersistentGameTimeSanitizer {
    private PersistentGameTimeSanitizer() {
    }

    public static long clampFutureUntil(long now, long storedUntil, long maxRemainingTicks) {
        if (maxRemainingTicks <= 0L) {
            return storedUntil;
        }

        var limit = now + maxRemainingTicks;
        return Math.min(storedUntil, limit);
    }

    public static long repairPersistedFutureUntil(long now, long storedUntil, long knownMaxRemainingTicks) {
        if (knownMaxRemainingTicks > 0L) {
            return clampFutureUntil(now, storedUntil, knownMaxRemainingTicks);
        }

        return clampFutureUntil(now, storedUntil, ApprenticeCodexServerConfig.savedAbsoluteTickClampMaxTicks());
    }

    public static long repairPersistedFutureUntilWithKnownMax(long now, long storedUntil, long knownMaxRemainingTicks) {
        return Math.min(storedUntil, now + Math.max(0L, knownMaxRemainingTicks));
    }

    public static long clampFutureStart(long now, long storedStart) {
        return Math.min(storedStart, now);
    }
}
