package jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void spellCalibrationBenchStoresScrollsOnGauntlet(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.spellCalibrationBenchStoresScrollsOnGauntlet(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletEmptySelectionViewsTrackEnabledSlots(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.scrollcasterGauntletEmptySelectionViewsTrackEnabledSlots(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletSelectedScrollDrivesImbuedSpell(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.scrollcasterGauntletSelectedScrollDrivesImbuedSpell(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletCastModeWorkbenchToggle(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.scrollcasterGauntletCastModeWorkbenchToggle(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletFreecastStaffAdjustmentEnablesSwingcast(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.scrollcasterGauntletFreecastStaffAdjustmentEnablesSwingcast(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletEpicFightMirroredOffhandSwingcastUsesMainhand(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.scrollcasterGauntletEpicFightMirroredOffhandSwingcastUsesMainhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletEpicFightFallbackIgnoresUnadjustedGauntlet(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.scrollcasterGauntletEpicFightFallbackIgnoresUnadjustedGauntlet(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCalibrationBenchAcceptsGauntletFreecastStaffAdjustment(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.spellCalibrationBenchAcceptsGauntletFreecastStaffAdjustment(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletStopsCreativeBlockAttackLikeVanillaSword(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.scrollcasterGauntletStopsCreativeBlockAttackLikeVanillaSword(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCalibrationBenchAdjustmentSlotsValidateInputs(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.spellCalibrationBenchAdjustmentSlotsValidateInputs(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellCalibrationBenchSchoolRuneRetunesGauntletSpellPower(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.spellCalibrationBenchSchoolRuneRetunesGauntletSpellPower(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletUsesNormalEnchantmentSurfaces(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.scrollcasterGauntletUsesNormalEnchantmentSurfaces(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletKeepsLegacyEnchantmentsAndAllowsBookExtraction(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.scrollcasterGauntletKeepsLegacyEnchantmentsAndAllowsBookExtraction(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletGrindstoneRemovesEnchantments(GameTestHelper helper) {
        ScrollcasterGauntletScenarios.scrollcasterGauntletGrindstoneRemovesEnchantments(helper);
    }
}
