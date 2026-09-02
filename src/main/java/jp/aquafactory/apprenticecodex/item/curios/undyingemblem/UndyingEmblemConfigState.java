package jp.aquafactory.apprenticecodex.item.curios.undyingemblem;

public final class UndyingEmblemConfigState {
    private static final int DEFAULT_RECONSTRUCTION_SPEED_MULTIPLIER = 10;
    private static int reconstructionSpeedMultiplier = DEFAULT_RECONSTRUCTION_SPEED_MULTIPLIER;

    private UndyingEmblemConfigState() {
    }

    public static int reconstructionSpeedMultiplier() {
        return reconstructionSpeedMultiplier;
    }

    public static void set(int value) {
        reconstructionSpeedMultiplier = Math.max(1, value);
    }

    public static void reset() {
        reconstructionSpeedMultiplier = DEFAULT_RECONSTRUCTION_SPEED_MULTIPLIER;
    }
}
