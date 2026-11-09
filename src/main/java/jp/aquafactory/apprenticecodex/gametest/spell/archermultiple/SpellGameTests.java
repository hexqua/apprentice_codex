package jp.aquafactory.apprenticecodex.gametest.spell.archermultiple;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.ARCHER_MULTIPLE_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @AfterBatch(batch = ARCHER_MULTIPLE_ISOLATED_BATCH)
    public static void cleanupArcherMultipleFixtures(ServerLevel level) {
        ArcherMultipleScenarios.cleanupFixtures();
    }

    @GameTest(template = TEMPLATE, batch = ARCHER_MULTIPLE_ISOLATED_BATCH)
    public static void archerMultipleInitialCastUsesFixedDurationAndSerializableLifecycle(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleInitialCastUsesFixedDurationAndSerializableLifecycle(helper);
    }

    @GameTest(template = TEMPLATE, batch = ARCHER_MULTIPLE_ISOLATED_BATCH)
    public static void archerMultipleTimeoutWithGreaterConjurersTalismanKeepsCooldown(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleTimeoutWithGreaterConjurersTalismanKeepsCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = ARCHER_MULTIPLE_ISOLATED_BATCH)
    public static void archerMultipleManualRecastRemovesBowsAndStartsCooldown(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleManualRecastRemovesBowsAndStartsCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = ARCHER_MULTIPLE_ISOLATED_BATCH)
    public static void archerMultipleCounterspellRemovesBowsAndStartsCooldown(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleCounterspellRemovesBowsAndStartsCooldown(helper);
    }

    @GameTest(template = TEMPLATE, batch = ARCHER_MULTIPLE_ISOLATED_BATCH)
    public static void archerMultipleLongTeleportKeepsFollowing(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleLongTeleportKeepsFollowing(helper);
    }

    @GameTest(template = TEMPLATE, batch = ARCHER_MULTIPLE_ISOLATED_BATCH)
    public static void archerMultipleDamageUsesSummonDamageAttribute(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleDamageUsesSummonDamageAttribute(helper);
    }

    @GameTest(template = TEMPLATE, batch = ARCHER_MULTIPLE_ISOLATED_BATCH, timeoutTicks = 60)
    public static void archerMultipleAllBowRemovalEndsRecastAndStartsCooldown(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleAllBowRemovalEndsRecastAndStartsCooldown(helper);
    }
}
