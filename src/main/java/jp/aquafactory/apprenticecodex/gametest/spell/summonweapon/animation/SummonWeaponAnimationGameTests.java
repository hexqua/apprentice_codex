package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.animation;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.SUMMON_WEAPON_ANIMATION_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SummonWeaponAnimationGameTests {
    private SummonWeaponAnimationGameTests() {}

    @GameTest(template = TEMPLATE, batch = SUMMON_WEAPON_ANIMATION_BATCH)
    public static void slashBladeStandbyAnimationSpeedTracksReducedCastTime(GameTestHelper helper) {
        SummonWeaponAnimationScenarios.slashBladeStandbyAnimationSpeedTracksReducedCastTime(helper);
    }

    @GameTest(template = TEMPLATE, batch = SUMMON_WEAPON_ANIMATION_BATCH)
    public static void moonLightStandbyAnimationSpeedAndDelayTrackReducedCastTime(GameTestHelper helper) {
        SummonWeaponAnimationScenarios.moonLightStandbyAnimationSpeedAndDelayTrackReducedCastTime(helper);
    }
}
