package jp.aquafactory.apprenticecodex.gametest.equipment.scythe;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.SPELL_REAPER_SCYTHE_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheUsesVanillaSweepWithoutMalum(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheUsesVanillaSweepWithoutMalum(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheUsesOneMalumSweepWithMalum(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheUsesOneMalumSweepWithMalum(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheIsRecognizedByMalumSoulDataHandler(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheIsRecognizedByMalumSoulDataHandler(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheDoesNotSweepWithNarrowEdge(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheDoesNotSweepWithNarrowEdge(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheDoesNotSweepWithHiddenBlade(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheDoesNotSweepWithHiddenBlade(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheRightClickWithoutAscensionIsNoOp(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheRightClickWithoutAscensionIsNoOp(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheRightClickWithReboundThrowsImmediately(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheRightClickWithReboundThrowsImmediately(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheRightClickTriggersMalumAscension(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheRightClickTriggersMalumAscension(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheAscensionLevelControlsManaCost(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionLevelControlsManaCost(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheAscensionRejectsInsufficientMana(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionRejectsInsufficientMana(helper);
    }

    @GameTest(template = TEMPLATE, batch = SPELL_REAPER_SCYTHE_CONFIG_BATCH)
    public static void spellReaperScytheAscensionUsesServerConfigAndZeroFloor(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionUsesServerConfigAndZeroFloor(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheAscensionCreativeBypassesManaAndCooldown(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionCreativeBypassesManaAndCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheAscensionTooltipShowsTranslatedNameAndMana(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionTooltipShowsTranslatedNameAndMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheAscensionWinsOverForcedRebound(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionWinsOverForcedRebound(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheAscensionUsesMalumCurios(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheAscensionUsesMalumCurios(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheBetterCombatKeepsNormalComboWhenSweepAllowed(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheBetterCombatKeepsNormalComboWhenSweepAllowed(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellReaperScytheBetterCombatUsesNoSweepCombo(GameTestHelper helper) {
        SpellReaperScytheGameTestScenarios.spellReaperScytheBetterCombatUsesNoSweepCombo(helper);
    }
}
