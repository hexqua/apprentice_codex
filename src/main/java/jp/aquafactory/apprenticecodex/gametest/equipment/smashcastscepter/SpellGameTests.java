package jp.aquafactory.apprenticecodex.gametest.equipment.smashcastscepter;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void smashcastScepterWindBurstUsesVanillaPostAttackEffect(GameTestHelper helper) {
        SmashcastScepterScenarios.smashcastScepterWindBurstUsesVanillaPostAttackEffect(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.assist_wings_isolated")
    public static void smashcastScepterSmashSetsVanillaImpulseFallProtection(GameTestHelper helper) {
        SmashcastScepterScenarios.smashcastScepterSmashSetsVanillaImpulseFallProtection(helper);
    }
}
