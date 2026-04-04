package jp.aquafactory.apprenticecodex.effect;

public final class CastingMoveSpeedAdjustment {
    public static final double MAX_CASTING_MOVE_SPEED_BONUS = 0.8D;

    private CastingMoveSpeedAdjustment() {
    }

    public static double computeAvailableBonus(double externalBonus) {
        var clampedExternalBonus = Math.max(0.0D, externalBonus);
        return Math.max(
                0.0D,
                Math.min(MAX_CASTING_MOVE_SPEED_BONUS, MAX_CASTING_MOVE_SPEED_BONUS - clampedExternalBonus)
        );
    }

    public static double computeSharedBonus(
            double externalBonus,
            double ownTargetBonus,
            double totalTargetBonus
    ) {
        if (ownTargetBonus <= 0.0D || totalTargetBonus <= 0.0D) {
            return 0.0D;
        }

        return computeAvailableBonus(externalBonus) * ownTargetBonus / totalTargetBonus;
    }
}
