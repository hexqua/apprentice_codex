package jp.aquafactory.apprenticecodex.gametest.spell.higanbana;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.HIGANBANA_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = HIGANBANA_ISOLATED_BATCH)
    public static void higanbanaHasNoRecastAndKeepsSlashCounts(GameTestHelper helper) {
        HiganbanaGameTestScenarios.higanbanaHasNoRecastAndKeepsSlashCounts(helper);
    }

    @GameTest(template = TEMPLATE, batch = HIGANBANA_ISOLATED_BATCH)
    public static void higanbanaAutomaticallySlashesWithoutFollowingOwner(GameTestHelper helper) {
        HiganbanaGameTestScenarios.higanbanaAutomaticallySlashesWithoutFollowingOwner(helper);
    }

    @GameTest(template = TEMPLATE, batch = HIGANBANA_ISOLATED_BATCH)
    public static void higanbanaDamageHasNoKnockbackAndHealsHalf(GameTestHelper helper) {
        HiganbanaGameTestScenarios.higanbanaDamageHasNoKnockbackAndHealsHalf(helper);
    }
}
