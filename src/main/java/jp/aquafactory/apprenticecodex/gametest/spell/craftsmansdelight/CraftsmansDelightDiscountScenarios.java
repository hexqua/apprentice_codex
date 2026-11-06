package jp.aquafactory.apprenticecodex.gametest.spell.craftsmansdelight;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightCooldownReductionEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.spell.ICraftsmansDelightAffectedSpell;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayer;

public final class CraftsmansDelightDiscountScenarios {
    private CraftsmansDelightDiscountScenarios() {}

    public static void assertCraftsmansDelightBasicDiscounts(
            GameTestHelper helper,
            FakePlayer player,
            AbstractSpell spell,
            int baseManaCost,
            String spellName,
            boolean expectedCastingMobility
    ) {
        if (!(spell instanceof ICraftsmansDelightAffectedSpell affectedSpell)) {
            helper.fail(spellName + " should opt into CraftsmansDelight support");
            return;
        }

        helper.assertFalse(affectedSpell.isCraftsmansDelightBreakSpeedBonusEnabled(),
                spellName + " should not receive CraftsmansDelight break speed bonuses");
        helper.assertFalse(affectedSpell.isCraftsmansDelightProcessSpeedBonusEnabled(),
                spellName + " should not receive CraftsmansDelight process speed bonuses");
        helper.assertTrue(affectedSpell.isCraftsmansDelightCastingMobilityEnabled() == expectedCastingMobility,
                spellName + " casting mobility should be " + expectedCastingMobility);
        helper.assertTrue(CraftsmansDelightSpellSupport.isManaCostDiscountTarget(spell.getSpellId()),
                spellName + " should be a CraftsmansDelight mana discount target");
        helper.assertTrue(CraftsmansDelightSpellSupport.isCooldownReductionTarget(spell),
                spellName + " should be a CraftsmansDelight cooldown reduction target");

        var manaEvent = new SpellOnCastEvent(
                player,
                spell.getSpellId(),
                1,
                baseManaCost,
                spell.getSchoolType(),
                CastSource.SPELLBOOK
        );
        CraftsmansDelightManaCostDiscountEvent.onSpellCast(manaEvent);
        var expectedManaCost = Math.max(1, Math.round(baseManaCost * 0.5f));
        helper.assertTrue(manaEvent.getManaCost() == expectedManaCost,
                spellName + " mana cost should be reduced to " + expectedManaCost + " but got " + manaEvent.getManaCost());

        var cooldownEvent = new SpellCooldownAddedEvent.Pre(
                spell.getSpellCooldown(),
                spell,
                player,
                CastSource.SPELLBOOK
        );
        CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(cooldownEvent);
        helper.assertTrue(cooldownEvent.getEffectiveCooldown()
                        == CraftsmansDelight.getReducedEffectiveCooldown(spell, player, CastSource.SPELLBOOK),
                spellName + " cooldown should route through the reduced cooldown helper");
    }
}
