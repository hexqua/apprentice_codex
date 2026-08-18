package jp.aquafactory.apprenticecodex.item;

public final class SchoolRuneSpellPowerTuning {
    public static final double BASE_GENERAL_SPELL_POWER_BONUS = 0.10D;
    public static final double TUNED_GENERAL_SPELL_POWER_BONUS = 0.05D;
    public static final double TUNED_SCHOOL_SPELL_POWER_BONUS = 0.15D;
    public static final double GENERAL_SPELL_POWER_REDUCTION =
            BASE_GENERAL_SPELL_POWER_BONUS - TUNED_GENERAL_SPELL_POWER_BONUS;

    private SchoolRuneSpellPowerTuning() {
    }
}
