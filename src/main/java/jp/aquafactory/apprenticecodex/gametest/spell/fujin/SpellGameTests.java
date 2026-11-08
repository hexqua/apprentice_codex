package jp.aquafactory.apprenticecodex.gametest.spell.fujin;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.FUJIN_ISOLATED_BATCH;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    // 射程と消滅までの飛行をtemplate内に収め、周辺地形との衝突を寿命終了と混同しない。
    private static final String TEMPLATE = "gametest/projectile_flight";

    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinKatanaFollowsOwnerAndReleases(GameTestHelper helper) {
        FujinGameTestScenarios.fujinKatanaFollowsOwnerAndReleases(helper);
    }

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinSlashPiercesAndDamagesEachTargetOnceWithoutKnockback(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashPiercesAndDamagesEachTargetOnceWithoutKnockback(helper);
    }

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinSlashUsesSmallBlockCollision(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashUsesSmallBlockCollision(helper);
    }

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinSlashDamagesTargetBeforeWall(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashDamagesTargetBeforeWall(helper);
    }

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinSlashRespectsParallelWallOcclusion(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashRespectsParallelWallOcclusion(helper);
    }

    @GameTest(template = TEMPLATE, batch = FUJIN_ISOLATED_BATCH)
    public static void fujinSlashExpiresBeyondRangeAndSupportsAntiMagic(GameTestHelper helper) {
        FujinGameTestScenarios.fujinSlashExpiresBeyondRangeAndSupportsAntiMagic(helper);
    }
}
