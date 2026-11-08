package jp.aquafactory.apprenticecodex.gametest.equipment.shield;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void bulwarkGreatshieldKeepsCoreItemAndEnchantmentContract(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.bulwarkGreatshieldKeepsCoreItemAndEnchantmentContract(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void bulwarkGreatshieldCalibrationStacksRepeatedSchoolRunes(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.bulwarkGreatshieldCalibrationStacksRepeatedSchoolRunes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void imbueShieldsIgnoreLegacyTranscendenceAndApplyWisdom(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.imbueShieldsIgnoreLegacyTranscendenceAndApplyWisdom(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void bulwarkGreatshieldDurabilityAndManaRateLimitsStayMemoryOnly(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.bulwarkGreatshieldDurabilityAndManaRateLimitsStayMemoryOnly(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void continuousShieldCastCleanupPreservesUseAndClearsLogoutState(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.continuousShieldCastCleanupPreservesUseAndClearsLogoutState(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void continuousShieldCastDurationsProgressMonotonically(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.continuousShieldCastDurationsProgressMonotonically(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void continuousShieldDeathClearsRuntimeWithNormalCooldown(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.continuousShieldDeathClearsRuntimeWithNormalCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void continuousShieldCreativeFinishSkipsCooldown(GameTestHelper helper) {
        BulwarkGreatshieldGameTestScenarios.continuousShieldCreativeFinishSkipsCooldown(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void parrycastBucklerKeepsCoreContract(GameTestHelper helper) {
        ParrycastBucklerGameTestScenarios.parrycastBucklerKeepsCoreContract(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void parrycastBucklerKeepsThreeAdjustmentsWithoutSchoolRunePower(GameTestHelper helper) {
        ParrycastBucklerGameTestScenarios.parrycastBucklerKeepsThreeAdjustmentsWithoutSchoolRunePower(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void parrycastBucklerKeepsPerfectGuardWindowAndDurabilityRateLimit(GameTestHelper helper) {
        ParrycastBucklerGameTestScenarios.parrycastBucklerKeepsPerfectGuardWindowAndDurabilityRateLimit(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void parrycastWisdomOnlyReducesAllCooldownsWhenSelectedSpellIsCoolingDown(GameTestHelper helper) {
        ParrycastBucklerGameTestScenarios.parrycastWisdomOnlyReducesAllCooldownsWhenSelectedSpellIsCoolingDown(helper);
    }
}
