package jp.aquafactory.apprenticecodex.gametest.spell.totemofpermafrost;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.totem_of_permafrost_isolated")
    public static void totemOfPermafrostCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostCanBePlacedOnSupportedSlab(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.totem_of_permafrost_isolated", timeoutTicks = 80)
    public static void totemOfPermafrostFallsWhenSupportRemoved(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostFallsWhenSupportRemoved(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.totem_of_permafrost_isolated")
    public static void totemOfPermafrostInvalidPlacementCreatesNoRecast(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostInvalidPlacementCreatesNoRecast(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.totem_of_permafrost_isolated")
    public static void totemOfPermafrostRecastRemovesPlacedTotem(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostRecastRemovesPlacedTotem(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.totem_of_permafrost_isolated")
    public static void totemOfPermafrostMissingTotemRecastIsNoop(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostMissingTotemRecastIsNoop(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.totem_of_permafrost_isolated")
    public static void totemOfPermafrostTimeoutRemovesPlacedTotem(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostTimeoutRemovesPlacedTotem(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.totem_of_permafrost_isolated")
    public static void totemOfPermafrostGreaterConjurersTalismanKeepsTimeoutCooldown(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostGreaterConjurersTalismanKeepsTimeoutCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.totem_of_permafrost_isolated")
    public static void totemOfPermafrostGreaterConjurersTalismanKeepsManualRecastCooldown(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostGreaterConjurersTalismanKeepsManualRecastCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.totem_of_permafrost_isolated")
    public static void totemOfPermafrostPulseUsesSummonDamageAttribute(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostPulseUsesSummonDamageAttribute(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.totem_of_permafrost_pulse", timeoutTicks = 40)
    public static void totemOfPermafrostPulseHitsVisibleCombatTargetsOnly(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostPulseHitsVisibleCombatTargetsOnly(helper);
    }
}
