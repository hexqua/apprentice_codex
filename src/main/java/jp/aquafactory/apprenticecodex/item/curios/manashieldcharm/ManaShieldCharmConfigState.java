package jp.aquafactory.apprenticecodex.item.curios.manashieldcharm;

public final class ManaShieldCharmConfigState {
    public static final float DEFAULT_MANA_PER_DAMAGE = 25.0F;
    public static final int DEFAULT_RECOVERY_THRESHOLD_MANA = 100;

    private static float manaPerDamage = DEFAULT_MANA_PER_DAMAGE;
    private static int recoveryThresholdMana = DEFAULT_RECOVERY_THRESHOLD_MANA;

    private ManaShieldCharmConfigState() {
    }

    public static float manaPerDamage() {
        return manaPerDamage;
    }

    public static int recoveryThresholdMana() {
        return recoveryThresholdMana;
    }

    public static void set(float manaPerDamage, int recoveryThresholdMana) {
        ManaShieldCharmConfigState.manaPerDamage = manaPerDamage;
        ManaShieldCharmConfigState.recoveryThresholdMana = recoveryThresholdMana;
    }

    public static void reset() {
        set(DEFAULT_MANA_PER_DAMAGE, DEFAULT_RECOVERY_THRESHOLD_MANA);
    }
}
