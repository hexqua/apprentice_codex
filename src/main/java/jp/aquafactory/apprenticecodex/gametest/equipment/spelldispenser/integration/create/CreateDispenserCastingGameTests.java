package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.integration.create;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.CREATE_COOLDOWN_MULTIPLIER_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CreateDispenserCastingGameTests {
    private CreateDispenserCastingGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateContinuousCastRequiresDisableBeforeRestart(GameTestHelper helper) {
        CreateDispenserCastingScenarios.spellDispenserCreateContinuousCastRequiresDisableBeforeRestart(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateContinuousCastStopsWhenDisabled(GameTestHelper helper) {
        CreateDispenserCastingScenarios.spellDispenserCreateContinuousCastStopsWhenDisabled(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateInstantCastUsesCooldown(GameTestHelper helper) {
        CreateDispenserCastingScenarios.spellDispenserCreateInstantCastUsesCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = CREATE_COOLDOWN_MULTIPLIER_CONFIG_BATCH)
    public static void spellDispenserCreateAppliesCooldownMultiplier(GameTestHelper helper) {
        CreateDispenserCastingScenarios.spellDispenserCreateAppliesCooldownMultiplier(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCreateAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        CreateDispenserCastingScenarios.spellDispenserCreateAllowsOwnerOptionalSpellWithoutOwnerProfile(helper);
    }
}
