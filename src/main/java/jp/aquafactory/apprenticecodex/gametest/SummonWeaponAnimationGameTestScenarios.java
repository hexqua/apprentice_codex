package jp.aquafactory.apprenticecodex.gametest;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.mixin.MagicDataAccessor;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.moonlight.MoonLight;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBlade;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import static jp.aquafactory.apprenticecodex.gametest.BowGameTestSupport.createEquipmentTestPlayer;

final class SummonWeaponAnimationGameTestScenarios {
    private static final UUID CAST_TIME_REDUCTION_TEST_MODIFIER_ID =
            UUID.fromString("1b0a34e0-55b6-42ad-b7a3-674934f70f1e");

    private SummonWeaponAnimationGameTestScenarios() {
    }

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
                    5.0f,
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

    private static void applyCastTimeReduction(GameTestHelper helper, net.minecraft.world.entity.LivingEntity player) {
        var castTimeReductionAttribute = player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION.get());
        helper.assertTrue(castTimeReductionAttribute != null,
                "Summon weapon animation test could not resolve cast-time reduction attribute");
        if (castTimeReductionAttribute == null) {
            return;
        }

        castTimeReductionAttribute.removeModifier(CAST_TIME_REDUCTION_TEST_MODIFIER_ID);
        castTimeReductionAttribute.addTransientModifier(new AttributeModifier(
                CAST_TIME_REDUCTION_TEST_MODIFIER_ID,
                "apprenticecodex.summon_weapon_animation_test",
                0.5D,
                AttributeModifier.Operation.ADDITION
        ));
    }

    private static void prepareLongCast(MagicData magicData, int castDuration) {
        var accessor = (MagicDataAccessor) magicData;
        accessor.apprenticecodex$setCastingSpellLevel(1);
        accessor.apprenticecodex$setCastDuration(castDuration);
        accessor.apprenticecodex$setCastDurationRemaining(castDuration);
        accessor.apprenticecodex$setCastSource(CastSource.SPELLBOOK);
        accessor.apprenticecodex$setCastType(CastType.LONG);
    }

    private static void assertFloatClose(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-4f,
                message + ". expected=" + expected + ", actual=" + actual);
    }
}
