package jp.aquafactory.apprenticecodex.gametest.spell.bulletstream;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.BULLET_STREAM_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = BULLET_STREAM_ISOLATED_BATCH)
    public static void bulletStreamWaitsForSpinUpThenFiresEveryTick(GameTestHelper helper) {
        BulletStreamGameTestScenarios.waitsForSpinUpThenFiresEveryTick(helper);
    }

    @GameTest(template = TEMPLATE, batch = BULLET_STREAM_ISOLATED_BATCH)
    public static void bulletStreamReleaseKeepsWeaponAndOnlyFinishesAfterFiring(GameTestHelper helper) {
        BulletStreamGameTestScenarios.releaseKeepsWeaponForTenTicksAndOnlyFinishesAfterFiring(helper);
    }

    @GameTest(template = TEMPLATE, batch = BULLET_STREAM_ISOLATED_BATCH)
    public static void bulletStreamCastDurationIgnoresCastTimeReduction(GameTestHelper helper) {
        BulletStreamGameTestScenarios.castDurationIgnoresCastTimeReduction(helper);
    }
}
