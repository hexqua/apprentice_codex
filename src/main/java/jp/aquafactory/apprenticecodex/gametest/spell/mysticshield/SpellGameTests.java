package jp.aquafactory.apprenticecodex.gametest.spell.mysticshield;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.MYSTIC_SHIELD_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = MYSTIC_SHIELD_ISOLATED_BATCH, timeoutTicks = 40)
    public static void mysticShieldBlocksFrontDamageAndLimitsSameSourceAccumulation(GameTestHelper helper) {
        MysticShieldScenarios.mysticShieldBlocksFrontDamageAndLimitsSameSourceAccumulation(helper);
    }

    @GameTest(template = TEMPLATE, batch = MYSTIC_SHIELD_ISOLATED_BATCH, timeoutTicks = 40)
    public static void mysticShieldReflectsStoredDamageAfterNonFrontCancel(GameTestHelper helper) {
        MysticShieldScenarios.mysticShieldReflectsStoredDamageAfterNonFrontCancel(helper);
    }

    @GameTest(template = TEMPLATE, batch = MYSTIC_SHIELD_ISOLATED_BATCH)
    public static void mysticShieldUsesYawWhenLookPitchIsVertical(GameTestHelper helper) {
        MysticShieldScenarios.mysticShieldUsesYawWhenLookPitchIsVertical(helper);
    }

    @GameTest(template = TEMPLATE, batch = MYSTIC_SHIELD_ISOLATED_BATCH)
    public static void mysticShieldReceivesProtectionSpellSupporterBenefits(GameTestHelper helper) {
        MysticShieldScenarios.mysticShieldReceivesProtectionSpellSupporterBenefits(helper);
    }
}
