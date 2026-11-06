package jp.aquafactory.apprenticecodex.gametest.spell.katana;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void horizontalOrientedBoxRejectsBroadPhaseCorners(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.horizontalOrientedBoxRejectsBroadPhaseCorners(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void orientedBoxOcclusionUsesPartialVisibilityAndEmbeddedSource(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.orientedBoxOcclusionUsesPartialVisibilityAndEmbeddedSource(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void orientedBoxDeduplicatesMultipartTargets(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.orientedBoxDeduplicatesMultipartTargets(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void slashBladeRefreshesPoseAndDamagesThroughWall(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.slashBladeRefreshesPoseAndDamagesThroughWall(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void slashBladeKeepsFullDamageAndKnockbackWithoutWall(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.slashBladeKeepsFullDamageAndKnockbackWithoutWall(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void thermalSliceRejectsWallHitsAndDamagesVisibleTarget(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.thermalSliceRejectsWallHitsAndDamagesVisibleTarget(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void shidenUsesTallNarrowAttackAreaAndCanBeInterrupted(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.shidenUsesTallNarrowAttackAreaAndCanBeInterrupted(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void shidenReducesDamageAndKnockbackThroughWall(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.shidenReducesDamageAndKnockbackThroughWall(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void higanbanaStaysHorizontalAndRejectsWallHits(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.higanbanaStaysHorizontalAndRejectsWallHits(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.katana_area_hit_isolated")
    public static void katanasKeepAttackOriginBeforeThinCover(GameTestHelper helper) {
        KatanaAreaHitGameTestScenarios.katanasKeepAttackOriginBeforeThinCover(helper);
    }
}
