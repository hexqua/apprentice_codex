package jp.aquafactory.apprenticecodex.gametest.support;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.earth_forge_isolated")
    public static void blockToolsTemporaryUseKeepsOneCountStackAndHands(GameTestHelper helper) {
        BlockUseScenarios.blockToolsTemporaryUseKeepsOneCountStackAndHands(helper);
    }
}
