package jp.aquafactory.apprenticecodex.gametest.spell.catchflame;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.CATCH_FLAME_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CatchFlameLegacyScenarios {
    private CatchFlameLegacyScenarios() {}

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameRejectsFireImmuneLivingTarget(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameRejectsFireImmuneLivingTarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = CATCH_FLAME_ISOLATED_BATCH)
    public static void catchFlameDamagesFireVulnerableEndCrystal(GameTestHelper helper) {
        CatchFlameGameTestScenarios.catchFlameDamagesFireVulnerableEndCrystal(helper);
    }
}
