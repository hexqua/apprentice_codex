package jp.aquafactory.apprenticecodex.gametest.spell.senseevil;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.sense_evil_isolated")
    public static void senseEvilUsesSameCubeForSpawnersAndEntities(GameTestHelper helper) {
        SenseEvilScenarios.senseEvilUsesSameCubeForSpawnersAndEntities(helper);
    }
}
