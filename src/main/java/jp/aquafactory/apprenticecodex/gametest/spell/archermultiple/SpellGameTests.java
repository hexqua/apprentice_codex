package jp.aquafactory.apprenticecodex.gametest.spell.archermultiple;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.archer_multiple_isolated")
    public static void archerMultipleInitialCastUsesFixedDurationAndSerializableLifecycle(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleInitialCastUsesFixedDurationAndSerializableLifecycle(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.archer_multiple_isolated")
    public static void archerMultipleTimeoutWithGreaterConjurersTalismanKeepsCooldown(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleTimeoutWithGreaterConjurersTalismanKeepsCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.archer_multiple_isolated")
    public static void archerMultipleManualRecastRemovesBowsAndStartsCooldown(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleManualRecastRemovesBowsAndStartsCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.archer_multiple_isolated")
    public static void archerMultipleCounterspellRemovesBowsAndStartsCooldown(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleCounterspellRemovesBowsAndStartsCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.archer_multiple_isolated")
    public static void archerMultipleLongTeleportKeepsFollowing(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleLongTeleportKeepsFollowing(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.archer_multiple_isolated")
    public static void archerMultipleDamageUsesSummonDamageAttribute(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleDamageUsesSummonDamageAttribute(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.archer_multiple_isolated", timeoutTicks = 60)
    public static void archerMultipleAllBowRemovalEndsRecastAndStartsCooldown(GameTestHelper helper) {
        ArcherMultipleScenarios.archerMultipleAllBowRemovalEndsRecastAndStartsCooldown(helper);
    }
}
