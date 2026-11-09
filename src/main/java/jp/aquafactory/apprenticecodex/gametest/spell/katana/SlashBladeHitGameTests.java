package jp.aquafactory.apprenticecodex.gametest.spell.katana;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.KATANA_AREA_HIT_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SlashBladeHitGameTests {
    private SlashBladeHitGameTests() {}

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void slashBladeRefreshesPoseAndDamagesThroughWall(GameTestHelper helper) {
        SlashBladeHitScenarios.slashBladeRefreshesPoseAndDamagesThroughWall(helper);
    }

    @GameTest(template = TEMPLATE, batch = KATANA_AREA_HIT_ISOLATED_BATCH)
    public static void slashBladeKeepsFullDamageAndKnockbackWithoutWall(GameTestHelper helper) {
        SlashBladeHitScenarios.slashBladeKeepsFullDamageAndKnockbackWithoutWall(helper);
    }
}
