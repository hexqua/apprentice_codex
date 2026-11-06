package jp.aquafactory.apprenticecodex.gametest.equipment.scythe;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheUsesVanillaSweepWithoutMalum(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheUsesVanillaSweepWithoutMalum(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheUsesOneMalumSweepWithMalum(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheUsesOneMalumSweepWithMalum(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheIsRecognizedByMalumSoulDataHandler(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheIsRecognizedByMalumSoulDataHandler(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheDoesNotSweepWithNarrowEdge(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheDoesNotSweepWithNarrowEdge(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheDoesNotSweepWithHiddenBlade(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheDoesNotSweepWithHiddenBlade(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheRightClickWithoutAscensionIsNoOp(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheRightClickWithoutAscensionIsNoOp(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheRightClickWithReboundThrowsImmediately(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheRightClickWithReboundThrowsImmediately(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheRightClickTriggersMalumAscension(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheRightClickTriggersMalumAscension(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheAscensionLevelControlsManaCost(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionLevelControlsManaCost(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheAscensionRejectsInsufficientMana(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionRejectsInsufficientMana(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.spell_reaper_scythe_config")
    public static void spellReaperScytheAscensionUsesServerConfigAndZeroFloor(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionUsesServerConfigAndZeroFloor(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheAscensionCreativeBypassesManaAndCooldown(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionCreativeBypassesManaAndCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheAscensionTooltipShowsTranslatedNameAndMana(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionTooltipShowsTranslatedNameAndMana(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheAscensionWinsOverForcedRebound(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionWinsOverForcedRebound(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheAscensionUsesMalumCurios(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionUsesMalumCurios(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheBetterCombatKeepsNormalComboWhenSweepAllowed(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheBetterCombatKeepsNormalComboWhenSweepAllowed(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void spellReaperScytheBetterCombatUsesNoSweepCombo(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheBetterCombatUsesNoSweepCombo(helper);
    }
}
