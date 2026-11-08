package jp.aquafactory.apprenticecodex.gametest.equipment.quiver;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void spellcasterQuiverUsesBackAndBeltSlotsAndCapsStoredArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.spellcasterQuiverUsesBackAndBeltSlotsAndCapsStoredArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void spellcasterQuiverAllowsOnlyOneEquippedAcrossSlots(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.spellcasterQuiverAllowsOnlyOneEquippedAcrossSlots(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void equippedSpellcasterQuiverAutoStoresPickedUpArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.equippedSpellcasterQuiverAutoStoresPickedUpArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void elementalBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void elementalBowMagicModeUsesConfiguredSpellcasterQuiverCatalyst(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowMagicModeUsesConfiguredSpellcasterQuiverCatalyst(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void elementalBowSelectionViewsIncludeSpellcasterQuiverArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowSelectionViewsIncludeSpellcasterQuiverArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void spellcasterQuiverSlowdownHelperTracksEquippedBowUse(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.spellcasterQuiverSlowdownHelperTracksEquippedBowUse(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void spellcasterQuiverSlowdownHelperTracksFocusStaffbowDrawUse(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.spellcasterQuiverSlowdownHelperTracksFocusStaffbowDrawUse(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void focusStaffbowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.focusStaffbowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void bowAmmoNotificationCountsExactArrowsAcrossInventoryAndQuiver(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.bowAmmoNotificationCountsExactArrowsAcrossInventoryAndQuiver(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spellcaster_quiver_isolated")
    public static void focusStaffbowAmmoConsumptionResultDistinguishesConsumptionFromBypass(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.focusStaffbowAmmoConsumptionResultDistinguishesConsumptionFromBypass(helper);
    }
}
