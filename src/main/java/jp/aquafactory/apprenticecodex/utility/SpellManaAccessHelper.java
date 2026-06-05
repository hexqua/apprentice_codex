package jp.aquafactory.apprenticecodex.utility;

import io.redspace.ironsspellbooks.api.spells.SpellData;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import org.jetbrains.annotations.NotNull;

public final class SpellManaAccessHelper {
    public static final int MAX_MANA = 1000;

    private SpellManaAccessHelper() {
    }

    public static int clampMana(int mana) {
        return Math.max(0, Math.min(MAX_MANA, mana));
    }

    public static int getSpellManaCost(SpellData spellData) {
        if (spellData == SpellData.EMPTY) {
            return 0;
        }

        return Math.max(0, spellData.getSpell().getManaCost(spellData.getLevel()));
    }

    public static boolean canAffordSpell(int currentMana, SpellData spellData) {
        return currentMana >= getSpellManaCost(spellData);
    }

    public static boolean canAffordSpell(@NotNull ManaAccess manaAccess, SpellData spellData) {
        return manaAccess.isManaConsumptionExempt() || canAffordSpell(manaAccess.getCurrentMana(), spellData);
    }

    public static boolean tryConsumeSpellMana(@NotNull ManaAccess manaAccess, SpellData spellData) {
        if (manaAccess.isManaConsumptionExempt()) {
            return true;
        }

        var manaCost = getSpellManaCost(spellData);
        if (manaCost <= 0) {
            return true;
        }

        var currentMana = clampMana(manaAccess.getCurrentMana());
        if (currentMana < manaCost) {
            return false;
        }

        manaAccess.setCurrentMana(currentMana - manaCost);
        return true;
    }

    public interface ManaAccess {
        int getCurrentMana();

        void setCurrentMana(int mana);

        default boolean isManaConsumptionExempt() {
            return false;
        }

        default double cooldownMultiplier() {
            return ApprenticeCodexServerConfig.spellDispenserCooldownMultiplier();
        }
    }
}
