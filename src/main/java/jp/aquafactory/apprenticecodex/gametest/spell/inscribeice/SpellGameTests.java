package jp.aquafactory.apprenticecodex.gametest.spell.inscribeice;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.inscribe_ice_isolated")
    public static void inscribeIceCastUsesShortThrowJob(GameTestHelper helper) {
        InscribeIceScenarios.inscribeIceCastUsesShortThrowJob(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.inscribe_ice_isolated")
    public static void notchedFrozenStacksAndBurstsOnThirdStack(GameTestHelper helper) {
        InscribeIceScenarios.notchedFrozenStacksAndBurstsOnThirdStack(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.inscribe_ice_isolated")
    public static void notchedFrozenMaintainsExistingFreezeWithoutIceWeakness(GameTestHelper helper) {
        InscribeIceScenarios.notchedFrozenMaintainsExistingFreezeWithoutIceWeakness(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.inscribe_ice_isolated")
    public static void inscribeIceBurstUsesHalfDamageForChainedBurstsAndSkipsPlayers(GameTestHelper helper) {
        InscribeIceScenarios.inscribeIceBurstUsesHalfDamageForChainedBurstsAndSkipsPlayers(helper);
    }
}
