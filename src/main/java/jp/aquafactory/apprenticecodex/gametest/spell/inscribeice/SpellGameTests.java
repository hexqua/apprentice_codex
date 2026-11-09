package jp.aquafactory.apprenticecodex.gametest.spell.inscribeice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.spell.casting.ApprenticeCodexSpellBehaviorGameTests.INSCRIBE_ICE_ISOLATED_BATCH;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = INSCRIBE_ICE_ISOLATED_BATCH)
    public static void inscribeIceCastUsesShortThrowJob(GameTestHelper helper) {
        InscribeIceScenarios.inscribeIceCastUsesShortThrowJob(helper);
    }

    @GameTest(template = TEMPLATE, batch = INSCRIBE_ICE_ISOLATED_BATCH)
    public static void notchedFrozenStacksAndBurstsOnThirdStack(GameTestHelper helper) {
        InscribeIceScenarios.notchedFrozenStacksAndBurstsOnThirdStack(helper);
    }

    @GameTest(template = TEMPLATE, batch = INSCRIBE_ICE_ISOLATED_BATCH)
    public static void notchedFrozenMaintainsExistingFreezeWithoutIceWeakness(GameTestHelper helper) {
        InscribeIceScenarios.notchedFrozenMaintainsExistingFreezeWithoutIceWeakness(helper);
    }

    @GameTest(template = TEMPLATE, batch = INSCRIBE_ICE_ISOLATED_BATCH)
    public static void inscribeIceBurstUsesHalfDamageForChainedBurstsAndSkipsPlayers(GameTestHelper helper) {
        InscribeIceScenarios.inscribeIceBurstUsesHalfDamageForChainedBurstsAndSkipsPlayers(helper);
    }
}
