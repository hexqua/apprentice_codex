package jp.aquafactory.apprenticecodex.block.spelldispenser;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Spell Dispenser の一時casterは通常のFakePlayerやLivingEntityと区別できないため、spell hook実行中だけ起動元を明示する。
 */
public final class SpellDispenserCastContext {
    private static final ThreadLocal<Deque<Boolean>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private SpellDispenserCastContext() {
    }

    public static boolean isActive() {
        return !STACK.get().isEmpty();
    }

    public static void run(Runnable action) {
        var stack = STACK.get();
        stack.push(Boolean.TRUE);
        try {
            action.run();
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                STACK.remove();
            }
        }
    }
}
