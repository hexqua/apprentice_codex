package jp.aquafactory.apprenticecodex.gametest.spell.summonweapon.animation;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import jp.aquafactory.apprenticecodex.mixin.MagicDataAccessor;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import static jp.aquafactory.apprenticecodex.gametest.spell.bulletstream.SummonWeaponAnimationGameTestScenarios.CAST_TIME_REDUCTION_TEST_MODIFIER_ID;

final class AnimationTimingLegacyScenarios {
    private AnimationTimingLegacyScenarios() {}

    static void applyCastTimeReduction(GameTestHelper helper, LivingEntity player) {
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

    static void prepareLongCast(MagicData magicData, int castDuration) {
        var accessor = (MagicDataAccessor) magicData;
        accessor.apprenticecodex$setCastingSpellLevel(1);
        accessor.apprenticecodex$setCastDuration(castDuration);
        accessor.apprenticecodex$setCastDurationRemaining(castDuration);
        accessor.apprenticecodex$setCastSource(CastSource.SPELLBOOK);
        accessor.apprenticecodex$setCastType(CastType.LONG);
    }

    static void assertFloatClose(GameTestHelper helper, float actual, float expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 1.0E-4f,
                message + ". expected=" + expected + ", actual=" + actual);
    }
}
