package jp.aquafactory.apprenticecodex.gametest.equipment.craftsmansdelight;

import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightCooldownReductionEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightManaCostDiscountEvent;
import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelightSpellSupport;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import static jp.aquafactory.apprenticecodex.gametest.equipment.craftsmansdelight.CraftsmansDelightLegacyScenarios.assertCraftsmansDelightBasicDiscounts;
import static jp.aquafactory.apprenticecodex.gametest.spell.heavenlyfist.HeavenlyFistHarvestGameTests.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class CraftsmansDelightLegacyScenariosSupport {
    private CraftsmansDelightLegacyScenariosSupport() {}

    static void craftsmansDelightAppliesToExternalSpellManaAndCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "craftsmans_external_spell_discount_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
            var touchDigSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.TOUCH_DIG.get();
            var spectralHammerSpell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.SPECTRAL_HAMMER_SPELL.get();

            var touchDigManaEvent = new SpellOnCastEvent(
                    player,
                    CraftsmansDelightSpellSupport.TOUCH_DIG_SPELL_ID,
                    1,
                    15,
                    touchDigSpell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightManaCostDiscountEvent.onSpellCast(touchDigManaEvent);
            helper.assertTrue(touchDigManaEvent.getManaCost() == 8,
                    "CraftsmansDelight should halve Touch Dig mana to 8 but got " + touchDigManaEvent.getManaCost());
            var expectedTouchDigBaseCooldown = Math.max(1, (int) (touchDigSpell.getSpellCooldown() * 0.5D));
            helper.assertTrue(CraftsmansDelight.applyCooldownDiscount(touchDigSpell.getSpellCooldown(), player) == expectedTouchDigBaseCooldown,
                    "CraftsmansDelight should apply the default 0.5 cooldown multiplier before player modifiers");

            var touchDigCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    10,
                    touchDigSpell,
                    player,
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(touchDigCooldownEvent);
            helper.assertTrue(touchDigCooldownEvent.getEffectiveCooldown()
                            == CraftsmansDelight.getReducedEffectiveCooldown(touchDigSpell, player, CastSource.SPELLBOOK),
                    "CraftsmansDelight should route Touch Dig cooldown through the reduced cooldown helper");

            var spectralHammerManaEvent = new SpellOnCastEvent(
                    player,
                    CraftsmansDelightSpellSupport.SPECTRAL_HAMMER_SPELL_ID,
                    1,
                    15,
                    spectralHammerSpell.getSchoolType(),
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightManaCostDiscountEvent.onSpellCast(spectralHammerManaEvent);
            helper.assertTrue(spectralHammerManaEvent.getManaCost() == 8,
                    "CraftsmansDelight should halve Spectral Hammer mana to 8 but got " + spectralHammerManaEvent.getManaCost());
            var expectedSpectralHammerBaseCooldown = Math.max(1, (int) (spectralHammerSpell.getSpellCooldown() * 0.5D));
            helper.assertTrue(CraftsmansDelight.applyCooldownDiscount(spectralHammerSpell.getSpellCooldown(), player) == expectedSpectralHammerBaseCooldown,
                    "CraftsmansDelight should apply the default 0.5 cooldown multiplier before player modifiers");

            var spectralHammerCooldownEvent = new SpellCooldownAddedEvent.Pre(
                    40,
                    spectralHammerSpell,
                    player,
                    CastSource.SPELLBOOK
            );
            CraftsmansDelightCooldownReductionEvent.onSpellCooldownAdded(spectralHammerCooldownEvent);
            helper.assertTrue(spectralHammerCooldownEvent.getEffectiveCooldown()
                            == CraftsmansDelight.getReducedEffectiveCooldown(spectralHammerSpell, player, CastSource.SPELLBOOK),
                    "CraftsmansDelight should route Spectral Hammer cooldown through the reduced cooldown helper");
        });
    }

    static void craftsmansDelightAppliesToHarvestMoonAndEarthForgeManaAndCooldown(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "craftsmans_apprentice_spell_discount_test");
            equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));

            assertCraftsmansDelightBasicDiscounts(helper, player, SpellRegistry.HARVEST_MOON.get(), 60, "Harvest Moon", true);
            assertCraftsmansDelightBasicDiscounts(helper, player, SpellRegistry.EARTH_FORGE.get(), 20, "Earth Forge", false);
        });
    }
}
