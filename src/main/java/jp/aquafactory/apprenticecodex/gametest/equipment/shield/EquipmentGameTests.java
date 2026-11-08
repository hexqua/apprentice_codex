package jp.aquafactory.apprenticecodex.gametest.equipment.shield;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void bulwarkGreatshieldKeepsCoreItemAndEnchantmentContract(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.bulwarkGreatshieldKeepsCoreItemAndEnchantmentContract(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void bulwarkGreatshieldCalibrationStacksRepeatedSchoolRunes(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.bulwarkGreatshieldCalibrationStacksRepeatedSchoolRunes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void imbueShieldsIgnoreLegacyTranscendenceAndApplyWisdom(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.imbueShieldsIgnoreLegacyTranscendenceAndApplyWisdom(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void bulwarkGreatshieldDurabilityAndManaRateLimitsStayMemoryOnly(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.bulwarkGreatshieldDurabilityAndManaRateLimitsStayMemoryOnly(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void continuousShieldCastCleanupPreservesUseAndClearsLogoutState(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.continuousShieldCastCleanupPreservesUseAndClearsLogoutState(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void continuousShieldCastDurationsProgressMonotonically(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.continuousShieldCastDurationsProgressMonotonically(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void continuousShieldDeathClearsRuntimeWithNormalCooldown(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.continuousShieldDeathClearsRuntimeWithNormalCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void continuousShieldCreativeFinishSkipsCooldown(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.continuousShieldCreativeFinishSkipsCooldown(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void parrycastBucklerKeepsCoreContract(GameTestHelper helper) {
        ParrycastBucklerGameTestScenarios.parrycastBucklerKeepsCoreContract(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void parrycastBucklerKeepsThreeAdjustmentsWithoutSchoolRunePower(GameTestHelper helper) {
        ParrycastBucklerGameTestScenarios.parrycastBucklerKeepsThreeAdjustmentsWithoutSchoolRunePower(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void parrycastBucklerKeepsPerfectGuardWindowAndDurabilityRateLimit(GameTestHelper helper) {
        ParrycastBucklerGameTestScenarios.parrycastBucklerKeepsPerfectGuardWindowAndDurabilityRateLimit(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void parrycastBucklerBlocksDispenserArrowAndStopsOnUnblockedDamage(GameTestHelper helper) {
        ParrycastBucklerGameTestScenarios.parrycastBucklerBlocksDispenserArrowAndStopsOnUnblockedDamage(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void parrycastWisdomOnlyReducesAllCooldownsWhenSelectedSpellIsCoolingDown(GameTestHelper helper) {
        ParrycastBucklerGameTestScenarios
                .parrycastWisdomOnlyReducesAllCooldownsWhenSelectedSpellIsCoolingDown(helper);
    }
}
