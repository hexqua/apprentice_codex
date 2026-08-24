package jp.aquafactory.apprenticecodex.compat.malum;

public final class MalumTouchDigSpellweavingContext {
    private static final ThreadLocal<Boolean> INITIAL_TOOL_MATCH_BYPASS =
            ThreadLocal.withInitial(() -> false);

    private MalumTouchDigSpellweavingContext() {
    }

    public static boolean isInitialToolMatchBypassed() {
        return INITIAL_TOOL_MATCH_BYPASS.get();
    }

    public static void runWithInitialToolMatchBypass(Runnable action) {
        var previous = INITIAL_TOOL_MATCH_BYPASS.get();
        INITIAL_TOOL_MATCH_BYPASS.set(true);
        try {
            action.run();
        } finally {
            if (previous) {
                INITIAL_TOOL_MATCH_BYPASS.set(true);
            } else {
                INITIAL_TOOL_MATCH_BYPASS.remove();
            }
        }
    }
}
