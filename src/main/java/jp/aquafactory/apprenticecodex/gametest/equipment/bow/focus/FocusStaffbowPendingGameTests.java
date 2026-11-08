package jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.focusstaffbow.FocusStaffbow;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.FOCUS_STAFFBOW_CONTINUOUS_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental.ElementalBowTestSupport.assertTranslatableKey;
import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.focus.BowGameTestSupport.FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class FocusStaffbowPendingGameTests {
    private FocusStaffbowPendingGameTests() {}

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowShowsLongSummonWeaponDuringPendingCast(GameTestHelper helper) {
        FocusStaffbowPendingScenarios.focusStaffbowShowsLongSummonWeaponDuringPendingCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowUpdatesArtisanSmashSplashRadiusOnChargedRelease(GameTestHelper helper) {
        FocusStaffbowPendingScenarios.focusStaffbowUpdatesArtisanSmashSplashRadiusOnChargedRelease(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowReevaluatesSummonWeaponAttackValuesOnChargedCast(GameTestHelper helper) {
        FocusStaffbowPendingScenarios.focusStaffbowReevaluatesSummonWeaponAttackValuesOnChargedCast(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(GameTestHelper helper) {
        FocusStaffbowPendingScenarios.focusStaffbowCancelsPendingSummonWeaponBeforeRequiredCharge(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowLethalAssaultWaitsForReleaseBeforeFiring(GameTestHelper helper) {
        FocusStaffbowPendingScenarios.focusStaffbowLethalAssaultWaitsForReleaseBeforeFiring(helper);
    }

    @GameTest(template = TEMPLATE, batch = FOCUS_STAFFBOW_CONTINUOUS_BATCH)
    public static void focusStaffbowHiganbanaWaitsForReleaseBeforeSlashing(GameTestHelper helper) {
        FocusStaffbowPendingScenarios.focusStaffbowHiganbanaWaitsForReleaseBeforeSlashing(helper);
    }

    static void castWithDoubledSpellPower(LivingEntity caster, Runnable cast) {
        var spellPowerAttribute = caster.getAttribute(AttributeRegistry.SPELL_POWER.get());
        if (spellPowerAttribute == null) {
            throw new IllegalStateException("Spell power attribute is unavailable");
        }
        var modifier = new AttributeModifier(
                FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID,
                "apprenticecodex.focus_staffbow.summon_weapon_attack_value_test",
                1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
        spellPowerAttribute.addTransientModifier(modifier);
        try {
            cast.run();
        } finally {
            spellPowerAttribute.removeModifier(FOCUS_STAFFBOW_OVERCHARGE_MODIFIER_ID);
        }
    }

    static void focusStaffbowInsufficientArrowMessageUsesExpectedTranslationKey(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var message = FocusStaffbow.createInsufficientArrowMessage();
            assertTranslatableKey(
                    helper,
                    message,
                    "ui.apprenticecodex.focus_staffbow.insufficient_arrow",
                    "Focus Staffbow insufficient arrow message should use the dedicated translation key"
            );
        });
    }


}
