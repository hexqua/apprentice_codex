package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.animation;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLight;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBlade;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.BowGameTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.animation.AnimationTimingLegacyScenarios.*;

final class SummonWeaponAnimationScenarios {
    private SummonWeaponAnimationScenarios() {}

    static void slashBladeStandbyAnimationSpeedTracksReducedCastTime(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "slash_blade_standby_speed_test");
            var spell = (SlashBlade) SpellRegistry.SLASH_BLADE.get();
            applyCastTimeReduction(helper, player);

            var baseCastTime = spell.getCastTime(1);
            var effectiveCastTime = spell.getEffectiveCastTime(1, player);
            helper.assertTrue(effectiveCastTime > 0 && effectiveCastTime < baseCastTime,
                    "Slash Blade test should have reduced cast time. base=" + baseCastTime
                            + ", effective=" + effectiveCastTime);

            var magicData = MagicData.getPlayerMagicData(player);
            prepareLongCast(magicData, effectiveCastTime);
            var weapon = spell.onCastNoWeapon(helper.getLevel(), 1, player, magicData);

            spell.onCastTickWithWeapon(helper.getLevel(), 1, player, magicData, weapon);
            assertFloatClose(
                    helper,
                    weapon.getAnimationSpeedForGameTest(),
                    1.5f * baseCastTime / effectiveCastTime,
                    "Slash Blade standby animation speed should track cast-time reduction"
            );

            spell.onCastCompleteWithWeapon(helper.getLevel(), 1, player, magicData, false, weapon);
            assertFloatClose(
                    helper,
                    weapon.getAnimationSpeedForGameTest(),
                    3.0f,
                    "Slash Blade quickdraw animation speed should keep its existing value"
            );
        });
    }

    static void moonLightStandbyAnimationSpeedAndDelayTrackReducedCastTime(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "moon_light_standby_speed_test");
            var spell = (MoonLight) SpellRegistry.MOON_LIGHT.get();
            applyCastTimeReduction(helper, player);

            var baseCastTime = spell.getCastTime(1);
            var effectiveCastTime = spell.getEffectiveCastTime(1, player);
            helper.assertTrue(effectiveCastTime > 0 && effectiveCastTime < baseCastTime,
                    "MoonLight test should have reduced cast time. base=" + baseCastTime
                            + ", effective=" + effectiveCastTime);

            var speedScale = baseCastTime / (float) effectiveCastTime;
            var expectedDelay = Mth.ceil(10 / speedScale);
            var magicData = MagicData.getPlayerMagicData(player);
            prepareLongCast(magicData, effectiveCastTime);
            var weapon = spell.onCastNoWeapon(helper.getLevel(), 1, player, magicData);

            weapon.tickCount = Math.max(0, expectedDelay - 1);
            spell.onCastTickWithWeapon(helper.getLevel(), 1, player, magicData, weapon);
            helper.assertFalse(weapon.isStandby(),
                    "MoonLight should not enter standby before the reduced standby delay");

            weapon.tickCount = expectedDelay;
            spell.onCastTickWithWeapon(helper.getLevel(), 1, player, magicData, weapon);
            helper.assertTrue(weapon.isStandby(),
                    "MoonLight should enter standby once the reduced standby delay is reached");
            assertFloatClose(
                    helper,
                    weapon.getAnimationSpeedForGameTest(),
                    1.5f * speedScale,
                    "MoonLight standby animation speed should track cast-time reduction"
            );

            spell.onCastCompleteWithWeapon(helper.getLevel(), 1, player, magicData, false, weapon);
            assertFloatClose(
                    helper,
                    weapon.getAnimationSpeedForGameTest(),
                    4.0f,
                    "MoonLight quickdraw animation speed should keep its existing value"
            );
        });
    }
}
