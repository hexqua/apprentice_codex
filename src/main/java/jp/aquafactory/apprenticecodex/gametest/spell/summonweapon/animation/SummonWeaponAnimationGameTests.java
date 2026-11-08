package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.animation;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLight;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBlade;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.animation.AnimationTimingFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SummonWeaponAnimationGameTests {
    private SummonWeaponAnimationGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void slashBladeStandbyAnimationSpeedTracksReducedCastTime(GameTestHelper helper) {
        var spell = (SlashBlade) SpellRegistry.SLASH_BLADE.get();
        var standby = new ArrayList<Float>();
        var release = new ArrayList<Float>();
        for (var testCase : REDUCTIONS) {
            try (var scene = new AnimationTimingFixture(helper, spell, testCase)) {
                var weapon = spell.onCastNoWeapon(helper.getLevel(), 1, scene.player, scene.data);
                scene.scope.assertVisible(weapon);
                spell.onCastTickWithWeapon(helper.getLevel(), 1, scene.player, scene.data, weapon);
                helper.assertTrue(weapon.isStandby(), "Slash Blade must enter standby: " + testCase.name());
                float speed = weapon.getAnimationSpeedForGameTest();
                assertClose(helper, speed, 1.5F * scene.baseCastTime / scene.effectiveCastTime,
                        "Slash Blade standby must track casting: " + testCase.name());
                standby.add(speed);
                scene.scope.run(() -> spell.onCastCompleteWithWeapon(helper.getLevel(), 1, scene.player, scene.data, false, weapon));
                release.add(weapon.getAnimationSpeedForGameTest());
            }
        }
        assertReleaseIndependent(helper, standby, release);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void moonLightStandbyAnimationSpeedAndDelayTrackReducedCastTime(GameTestHelper helper) {
        var spell = (MoonLight) SpellRegistry.MOON_LIGHT.get();
        var standby = new ArrayList<Float>();
        var release = new ArrayList<Float>();
        for (var testCase : REDUCTIONS) {
            try (var scene = new AnimationTimingFixture(helper, spell, testCase)) {
                float speedScale = scene.baseCastTime / (float) scene.effectiveCastTime;
                int expectedDelay = Mth.ceil(10 / speedScale);
                var weapon = spell.onCastNoWeapon(helper.getLevel(), 1, scene.player, scene.data);
                scene.scope.assertVisible(weapon);
                weapon.tickCount = Math.max(0, expectedDelay - 1);
                spell.onCastTickWithWeapon(helper.getLevel(), 1, scene.player, scene.data, weapon);
                helper.assertFalse(weapon.isStandby(), "Moon Light must wait until the shortened delay: " + testCase.name());
                weapon.tickCount = expectedDelay;
                spell.onCastTickWithWeapon(helper.getLevel(), 1, scene.player, scene.data, weapon);
                helper.assertTrue(weapon.isStandby(), "Moon Light must enter standby at the shortened delay: " + testCase.name());
                float speed = weapon.getAnimationSpeedForGameTest();
                assertClose(helper, speed, 1.5F * speedScale, "Moon Light standby must track casting: " + testCase.name());
                standby.add(speed);
                scene.scope.run(() -> spell.onCastCompleteWithWeapon(helper.getLevel(), 1, scene.player, scene.data, false, weapon));
                release.add(weapon.getAnimationSpeedForGameTest());
            }
        }
        assertReleaseIndependent(helper, standby, release);
        helper.succeed();
    }
}
