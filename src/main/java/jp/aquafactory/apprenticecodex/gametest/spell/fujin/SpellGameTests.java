package jp.aquafactory.apprenticecodex.gametest.spell.fujin;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.fujin_isolated")
    public static void fujinKatanaFollowsOwnerAndReleases(GameTestHelper helper) {
        FujinGameTestScenarios.fujinKatanaFollowsOwnerAndReleases(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.fujin_isolated")
    public static void fujinSlashPiercesAndDamagesEachTargetOnceWithoutKnockback(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashPiercesAndDamagesEachTargetOnceWithoutKnockback(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.fujin_isolated")
    public static void fujinSlashUsesSmallBlockCollision(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashUsesSmallBlockCollision(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.fujin_isolated")
    public static void fujinSlashDamagesTargetBeforeWall(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashDamagesTargetBeforeWall(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.fujin_isolated")
    public static void fujinSlashRespectsParallelWallOcclusion(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashRespectsParallelWallOcclusion(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.fujin_isolated")
    public static void fujinSlashExpiresBeyondRangeAndSupportsAntiMagic(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashExpiresBeyondRangeAndSupportsAntiMagic(helper);
    }
}
