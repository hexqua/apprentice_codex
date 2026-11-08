package jp.aquafactory.apprenticecodex.gametest.spell.higanbana;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.higanbana_isolated")
    public static void higanbanaHasNoRecastAndKeepsSlashCounts(GameTestHelper helper) {
        HiganbanaGameTestScenarios.higanbanaHasNoRecastAndKeepsSlashCounts(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.higanbana_isolated")
    public static void higanbanaAutomaticallySlashesWithoutFollowingOwner(GameTestHelper helper) {
        HiganbanaGameTestScenarios.higanbanaAutomaticallySlashesWithoutFollowingOwner(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.higanbana_isolated")
    public static void higanbanaDamageHasNoKnockbackAndHealsHalf(GameTestHelper helper) {
        HiganbanaGameTestScenarios.higanbanaDamageHasNoKnockbackAndHealsHalf(helper);
    }
}
