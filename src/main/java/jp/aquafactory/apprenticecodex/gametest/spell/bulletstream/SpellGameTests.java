package jp.aquafactory.apprenticecodex.gametest.spell.bulletstream;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.bullet_stream_isolated")
    public static void bulletStreamWaitsForSpinUpThenFiresEveryTick(GameTestHelper helper) {
        BulletStreamGameTestScenarios.waitsForSpinUpThenFiresEveryTick(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.bullet_stream_isolated")
    public static void bulletStreamReleaseKeepsWeaponAndOnlyFinishesAfterFiring(GameTestHelper helper) {
        BulletStreamGameTestScenarios.releaseKeepsWeaponForTenTicksAndOnlyFinishesAfterFiring(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.bullet_stream_isolated")
    public static void bulletStreamCastDurationIgnoresCastTimeReduction(GameTestHelper helper) {
        BulletStreamGameTestScenarios.castDurationIgnoresCastTimeReduction(helper);
    }
}
