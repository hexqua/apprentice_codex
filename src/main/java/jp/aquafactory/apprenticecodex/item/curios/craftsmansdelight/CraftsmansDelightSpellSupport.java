package jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;

import java.util.List;
import java.util.Set;

public final class CraftsmansDelightSpellSupport {
    public static final String TOUCH_DIG_SPELL_ID = "irons_spellbooks:touch_dig";
    public static final String SPECTRAL_HAMMER_SPELL_ID = "irons_spellbooks:spectral_hammer";

    private static final Set<String> EXTERNAL_MANA_COST_TARGETS = Set.of(
            TOUCH_DIG_SPELL_ID,
            SPECTRAL_HAMMER_SPELL_ID
    );
    private static final Set<String> EXTERNAL_COOLDOWN_TARGETS = Set.of(
            TOUCH_DIG_SPELL_ID,
            SPECTRAL_HAMMER_SPELL_ID
    );

    private CraftsmansDelightSpellSupport() {
    }

    public static boolean isManaCostDiscountTarget(String spellId) {
        if (EXTERNAL_MANA_COST_TARGETS.contains(spellId)) {
            return true;
        }

        var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(spellId);
        return spell instanceof ICraftsmansDelightAffectedSpell affectedSpell
                && affectedSpell.isCraftsmansDelightManaCostDiscountEnabled();
    }

    public static boolean isCooldownReductionTarget(AbstractSpell spell) {
        return spell instanceof ICraftsmansDelightAffectedSpell affectedSpell
                ? affectedSpell.isCraftsmansDelightCooldownReductionEnabled()
                : EXTERNAL_COOLDOWN_TARGETS.contains(spell.getSpellId());
    }

    public static List<AbstractSpell> getExternalTargetSpells() {
        return EXTERNAL_MANA_COST_TARGETS.stream()
                .map(io.redspace.ironsspellbooks.api.registry.SpellRegistry::getSpell)
                .toList();
    }
}
