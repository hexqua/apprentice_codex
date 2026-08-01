package jp.aquafactory.apprenticecodex.item.magicitem.client;

public final class InstantSearchBrazierConfigState {
    public static final int DEFAULT_INITIAL_RANGE = 500;

    private static int initialRange = DEFAULT_INITIAL_RANGE;

    private InstantSearchBrazierConfigState() {
    }

    public static int initialRange() {
        return initialRange;
    }

    public static void set(int initialRange) {
        InstantSearchBrazierConfigState.initialRange = initialRange;
    }

    public static void reset() {
        initialRange = DEFAULT_INITIAL_RANGE;
    }
}
