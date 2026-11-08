package jp.aquafactory.apprenticecodex.gametest.spell.mistform;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mist_form_isolated")
    public static void mistFormAppliesEffectAndFixedAttributes(GameTestHelper helper) {
        MistFormScenarios.mistFormAppliesEffectAndFixedAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mist_form_isolated")
    public static void mistFormSuppressesAwarenessWithinThirtyTwoBlocks(GameTestHelper helper) {
        MistFormScenarios.mistFormSuppressesAwarenessWithinThirtyTwoBlocks(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mist_form_isolated")
    public static void mistFormDamageToLivingTargetRemovesEffect(GameTestHelper helper) {
        MistFormScenarios.mistFormDamageToLivingTargetRemovesEffect(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mist_form_isolated")
    public static void mistFormSlowsFallingWithoutAmplifierScaling(GameTestHelper helper) {
        MistFormScenarios.mistFormSlowsFallingWithoutAmplifierScaling(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mist_form_isolated")
    public static void mistFormStandsOnLiquidAndSneakSinks(GameTestHelper helper) {
        MistFormScenarios.mistFormStandsOnLiquidAndSneakSinks(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mist_form_isolated")
    public static void mistFormPassesTaggedBlocksAndRejectsGlass(GameTestHelper helper) {
        MistFormScenarios.mistFormPassesTaggedBlocksAndRejectsGlass(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mist_form_isolated")
    public static void mistFormPassableBlockDenylistBlocksIdsAndTags(GameTestHelper helper) {
        MistFormScenarios.mistFormPassableBlockDenylistBlocksIdsAndTags(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mist_form_isolated")
    public static void mistFormWaterloggedPassableBlockDoesNotSnapUp(GameTestHelper helper) {
        MistFormScenarios.mistFormWaterloggedPassableBlockDoesNotSnapUp(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mist_form_isolated")
    public static void mistFormIgnoresTaggedMovementRestrictions(GameTestHelper helper) {
        MistFormScenarios.mistFormIgnoresTaggedMovementRestrictions(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mist_form_isolated")
    public static void mistFormMovementRestrictionIgnoreKeepsBlockEffects(GameTestHelper helper) {
        MistFormScenarios.mistFormMovementRestrictionIgnoreKeepsBlockEffects(helper);
    }
}
