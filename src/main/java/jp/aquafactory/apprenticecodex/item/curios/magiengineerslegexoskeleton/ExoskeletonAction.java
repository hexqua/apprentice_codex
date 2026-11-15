package jp.aquafactory.apprenticecodex.item.curios.magiengineerslegexoskeleton;

public enum ExoskeletonAction {
    LIFTER, GROUND_STRIDER, AIR_STRIDER;

    public boolean isStrider() {
        return this == GROUND_STRIDER || this == AIR_STRIDER;
    }

    public int manaCost(int lifterCost, int striderCost) {
        return isStrider() ? striderCost : lifterCost;
    }

    public static ExoskeletonAction select(boolean grounded, boolean sprinting, boolean lifterPressed,
                                           boolean striderPressed) {
        if (sprinting && striderPressed) return grounded ? GROUND_STRIDER : AIR_STRIDER;
        if (!grounded && lifterPressed) return LIFTER;
        return null;
    }
}
