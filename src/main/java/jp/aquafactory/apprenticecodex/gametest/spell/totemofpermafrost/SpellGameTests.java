package jp.aquafactory.apprenticecodex.gametest.spell.totemofpermafrost;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostCanBePlacedOnSupportedSlab(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH, timeoutTicks = 80)
    public static void totemOfPermafrostFallsWhenSupportRemoved(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostFallsWhenSupportRemoved(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostInvalidPlacementCreatesNoRecast(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostInvalidPlacementCreatesNoRecast(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostRecastRemovesPlacedTotem(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostRecastRemovesPlacedTotem(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostMissingTotemRecastIsNoop(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostMissingTotemRecastIsNoop(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostTimeoutRemovesPlacedTotem(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostTimeoutRemovesPlacedTotem(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostGreaterConjurersTalismanKeepsTimeoutCooldown(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostGreaterConjurersTalismanKeepsTimeoutCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostGreaterConjurersTalismanKeepsManualRecastCooldown(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostGreaterConjurersTalismanKeepsManualRecastCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_ISOLATED_BATCH)
    public static void totemOfPermafrostPulseUsesSummonDamageAttribute(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostPulseUsesSummonDamageAttribute(helper);
    }

    @GameTest(template = TEMPLATE, batch = TOTEM_OF_PERMAFROST_PULSE_BATCH, timeoutTicks = 40)
    public static void totemOfPermafrostPulseHitsVisibleCombatTargetsOnly(GameTestHelper helper) {
        TotemOfPermafrostScenarios.totemOfPermafrostPulseHitsVisibleCombatTargetsOnly(helper);
    }
}
