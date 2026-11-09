package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.casting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserActivationGameTests {
    private SpellDispenserActivationGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityStopsContinuousCastWhenSignalTurnsOff(GameTestHelper helper) {
        SpellDispenserActivationScenarios.spellDispenserBlockEntityStopsContinuousCastWhenSignalTurnsOff(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityDoesNotRestartContinuousCastWhileSignalStaysOn(GameTestHelper helper) {
        SpellDispenserActivationScenarios.spellDispenserBlockEntityDoesNotRestartContinuousCastWhileSignalStaysOn(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityMarksContinuousResetRequirementAfterHeldCompletion(GameTestHelper helper) {
        SpellDispenserActivationScenarios.spellDispenserBlockEntityMarksContinuousResetRequirementAfterHeldCompletion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityStartsAndClearsInstantCooldown(GameTestHelper helper) {
        SpellDispenserActivationScenarios.spellDispenserBlockEntityStartsAndClearsInstantCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = BLOCK_SERVER_DISABLED_CONFIG_BATCH)
    public static void spellDispenserBlockEntityReportsServerDisabledActivation(GameTestHelper helper) {
        SpellDispenserActivationScenarios.spellDispenserBlockEntityReportsServerDisabledActivation(helper);
    }

    @GameTest(template = TEMPLATE, batch = BLOCK_COOLDOWN_MULTIPLIER_CONFIG_BATCH)
    public static void spellDispenserBlockEntityAppliesCooldownMultiplier(GameTestHelper helper) {
        SpellDispenserActivationScenarios.spellDispenserBlockEntityAppliesCooldownMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserActivationFailsWhenManaIsInsufficient(GameTestHelper helper) {
        SpellDispenserActivationScenarios.spellDispenserActivationFailsWhenManaIsInsufficient(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserContinuousCastStopsWhenManaRunsOut(GameTestHelper helper) {
        SpellDispenserActivationScenarios.spellDispenserContinuousCastStopsWhenManaRunsOut(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityRejectsActivationWithoutOwnerProfile(GameTestHelper helper) {
        SpellDispenserActivationScenarios.spellDispenserBlockEntityRejectsActivationWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserBlockEntityAllowsOwnerOptionalActivationWithoutOwnerProfile(GameTestHelper helper) {
        SpellDispenserActivationScenarios.spellDispenserBlockEntityAllowsOwnerOptionalActivationWithoutOwnerProfile(helper);
    }
}
