package jp.aquafactory.apprenticecodex.gametest.spell.mysticshield;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mystic_shield_isolated", timeoutTicks = 40)
    public static void mysticShieldBlocksFrontDamageAndLimitsSameSourceAccumulation(GameTestHelper helper) {
        MysticShieldScenarios.mysticShieldBlocksFrontDamageAndLimitsSameSourceAccumulation(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mystic_shield_isolated", timeoutTicks = 40)
    public static void mysticShieldReflectsStoredDamageAfterNonFrontCancel(GameTestHelper helper) {
        MysticShieldScenarios.mysticShieldReflectsStoredDamageAfterNonFrontCancel(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mystic_shield_isolated")
    public static void mysticShieldUsesYawWhenLookPitchIsVertical(GameTestHelper helper) {
        MysticShieldScenarios.mysticShieldUsesYawWhenLookPitchIsVertical(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.mystic_shield_isolated")
    public static void mysticShieldReceivesProtectionSpellSupporterBenefits(GameTestHelper helper) {
        MysticShieldScenarios.mysticShieldReceivesProtectionSpellSupporterBenefits(helper);
    }
}
