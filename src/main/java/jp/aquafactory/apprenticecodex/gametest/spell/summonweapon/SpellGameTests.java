package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.summon_weapon_animation")
    public static void slashBladeStandbyAnimationSpeedTracksReducedCastTime(GameTestHelper helper) {
        SummonWeaponAnimationGameTestScenarios.slashBladeStandbyAnimationSpeedTracksReducedCastTime(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.summon_weapon_animation")
    public static void moonLightStandbyAnimationSpeedAndDelayTrackReducedCastTime(GameTestHelper helper) {
        SummonWeaponAnimationGameTestScenarios.moonLightStandbyAnimationSpeedAndDelayTrackReducedCastTime(helper);
    }
}
