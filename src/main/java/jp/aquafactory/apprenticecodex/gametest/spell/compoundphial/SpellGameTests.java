package jp.aquafactory.apprenticecodex.gametest.spell.compoundphial;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.COMPOUND_PHIAL_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = COMPOUND_PHIAL_ISOLATED_BATCH, timeoutTicks = 40)
    public static void compoundPhialSplashDamageUsesWeakFalloffAndKeepsSelfHit(GameTestHelper helper) {
        CompoundPhialScenarios.compoundPhialSplashDamageUsesWeakFalloffAndKeepsSelfHit(helper);
    }
}
