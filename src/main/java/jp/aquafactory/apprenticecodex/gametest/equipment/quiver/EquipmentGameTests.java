package jp.aquafactory.apprenticecodex.gametest.equipment.quiver;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.SPELLCASTER_QUIVER_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void focusStaffbowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.focusStaffbowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void bowAmmoNotificationCountsExactArrowsAcrossInventoryAndQuiver(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.bowAmmoNotificationCountsExactArrowsAcrossInventoryAndQuiver(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void focusStaffbowAmmoConsumptionResultDistinguishesConsumptionFromBypass(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.focusStaffbowAmmoConsumptionResultDistinguishesConsumptionFromBypass(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void spellcasterQuiverUsesBackAndBeltSlotsAndCapsStoredArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.spellcasterQuiverUsesBackAndBeltSlotsAndCapsStoredArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void spellcasterQuiverAllowsOnlyOneEquippedAcrossSlots(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.spellcasterQuiverAllowsOnlyOneEquippedAcrossSlots(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void equippedSpellcasterQuiverAutoStoresPickedUpArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.equippedSpellcasterQuiverAutoStoresPickedUpArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowMagicModeUsesConfiguredSpellcasterQuiverCatalyst(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowMagicModeUsesConfiguredSpellcasterQuiverCatalyst(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowSelectionViewsIncludeSpellcasterQuiverArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowSelectionViewsIncludeSpellcasterQuiverArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowConsumesSpellcasterQuiverArrowsBeforeInventory(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowPrefersHeldSpecialArrowOverQuiverNormalArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowPrefersNormalArrowOverMoreNumerousQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.vanillaBowInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowVanillaModePrefersHeldSpecialArrowOverQuiverNormalArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.elementalBowVanillaModeInfinityFallsBackToNormalArrowBeforeQuiverSpecialArrows(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void spellcasterQuiverSlowdownHelperTracksEquippedBowUse(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.spellcasterQuiverSlowdownHelperTracksEquippedBowUse(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELLCASTER_QUIVER_ISOLATED_BATCH)
    public static void spellcasterQuiverSlowdownHelperTracksFocusStaffbowDrawUse(GameTestHelper helper) {
        SpellcasterQuiverGameTestScenarios.spellcasterQuiverSlowdownHelperTracksFocusStaffbowDrawUse(helper);
    }
}
