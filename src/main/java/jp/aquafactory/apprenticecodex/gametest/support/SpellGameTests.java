package jp.aquafactory.apprenticecodex.gametest.support;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.EARTH_FORGE_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = EARTH_FORGE_ISOLATED_BATCH)
    public static void blockToolsTemporaryUseKeepsOneCountStackAndHands(GameTestHelper helper) {
        BlockUseScenarios.blockToolsTemporaryUseKeepsOneCountStackAndHands(helper);
    }
}
