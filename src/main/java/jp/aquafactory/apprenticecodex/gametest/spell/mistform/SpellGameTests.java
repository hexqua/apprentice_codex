package jp.aquafactory.apprenticecodex.gametest.spell.mistform;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.MIST_FORM_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormAppliesEffectAndFixedAttributes(GameTestHelper helper) {
        MistFormScenarios.mistFormAppliesEffectAndFixedAttributes(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormSuppressesAwarenessWithinThirtyTwoBlocks(GameTestHelper helper) {
        MistFormScenarios.mistFormSuppressesAwarenessWithinThirtyTwoBlocks(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormDamageToLivingTargetRemovesEffect(GameTestHelper helper) {
        MistFormScenarios.mistFormDamageToLivingTargetRemovesEffect(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormSlowsFallingWithoutAmplifierScaling(GameTestHelper helper) {
        MistFormScenarios.mistFormSlowsFallingWithoutAmplifierScaling(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormStandsOnLiquidAndSneakSinks(GameTestHelper helper) {
        MistFormScenarios.mistFormStandsOnLiquidAndSneakSinks(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormPassesTaggedBlocksAndRejectsGlass(GameTestHelper helper) {
        MistFormScenarios.mistFormPassesTaggedBlocksAndRejectsGlass(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormPassableBlockDenylistBlocksIdsAndTags(GameTestHelper helper) {
        MistFormScenarios.mistFormPassableBlockDenylistBlocksIdsAndTags(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormWaterloggedPassableBlockDoesNotSnapUp(GameTestHelper helper) {
        MistFormScenarios.mistFormWaterloggedPassableBlockDoesNotSnapUp(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormIgnoresTaggedMovementRestrictions(GameTestHelper helper) {
        MistFormScenarios.mistFormIgnoresTaggedMovementRestrictions(helper);
    }

    @GameTest(template = TEMPLATE, batch = MIST_FORM_ISOLATED_BATCH)
    public static void mistFormMovementRestrictionIgnoreKeepsBlockEffects(GameTestHelper helper) {
        MistFormScenarios.mistFormMovementRestrictionIgnoreKeepsBlockEffects(helper);
    }
}
