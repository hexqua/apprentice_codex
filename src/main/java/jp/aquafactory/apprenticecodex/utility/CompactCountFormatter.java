package jp.aquafactory.apprenticecodex.utility;

public final class CompactCountFormatter {
    private static final long COMPACT_THRESHOLD = 1000L;

    private CompactCountFormatter() {
    }

    public static String format(long count) {
        var normalizedCount = Math.max(0L, count);
        if (normalizedCount < COMPACT_THRESHOLD) {
            return Long.toString(normalizedCount);
        }
        return normalizedCount / COMPACT_THRESHOLD + "K";
    }

    public static String formatItemDecorationCount(int count) {
        if (count < COMPACT_THRESHOLD) {
            return null;
        }
        return format(count);
    }
}
