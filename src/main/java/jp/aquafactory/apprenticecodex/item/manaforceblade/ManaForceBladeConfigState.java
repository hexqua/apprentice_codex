package jp.aquafactory.apprenticecodex.item.manaforceblade;

public final class ManaForceBladeConfigState {
    public static final float DEFAULT_IMBUE_DAMAGE_MULTIPLIER_SCALE = 1.0F;
    public static final float DEFAULT_ATTACK_MANA_COST_MULTIPLIER = 3.0F;
    public static final float DEFAULT_ATTACK_MANA_SCHOOL_MULTIPLIER_SCALE = 1.0F;

    private static float imbueDamageMultiplierScale = DEFAULT_IMBUE_DAMAGE_MULTIPLIER_SCALE;
    private static float attackManaCostMultiplier = DEFAULT_ATTACK_MANA_COST_MULTIPLIER;
    private static float attackManaSchoolMultiplierScale = DEFAULT_ATTACK_MANA_SCHOOL_MULTIPLIER_SCALE;

    private ManaForceBladeConfigState() {
    }

    public static float imbueDamageMultiplierScale() {
        return imbueDamageMultiplierScale;
    }

    public static float attackManaCostMultiplier() {
        return attackManaCostMultiplier;
    }

    public static float attackManaSchoolMultiplierScale() {
        return attackManaSchoolMultiplierScale;
    }

    public static void set(
            float imbueDamageMultiplierScale,
            float attackManaCostMultiplier,
            float attackManaSchoolMultiplierScale
    ) {
        ManaForceBladeConfigState.imbueDamageMultiplierScale = imbueDamageMultiplierScale;
        ManaForceBladeConfigState.attackManaCostMultiplier = attackManaCostMultiplier;
        ManaForceBladeConfigState.attackManaSchoolMultiplierScale = attackManaSchoolMultiplierScale;
    }

    public static void reset() {
        set(
                DEFAULT_IMBUE_DAMAGE_MULTIPLIER_SCALE,
                DEFAULT_ATTACK_MANA_COST_MULTIPLIER,
                DEFAULT_ATTACK_MANA_SCHOOL_MULTIPLIER_SCALE
        );
    }
}
