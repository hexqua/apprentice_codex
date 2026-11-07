package jp.aquafactory.apprenticecodex.gametest.enchantment.attribute;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class AttributeEnchantmentGameTests {
    private AttributeEnchantmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void forcedAttributeEnchantmentsCoexistAndKeepRawPositiveLevels(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.forcedEnchantmentsCoexistAndKeepRawPositiveLevels(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void configuredAttributeEnchantmentAmountsScaleLinearly(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.configuredAmountsPerLevelScaleLinearly(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attunementResolutionFailureIsNoOp(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.attunementResolutionFailureIsNoOp(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magicAttributeMergeOnlyNormalizesLinearMagicAttributes(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.mergeHelperOnlyNormalizesLinearMagicAttributes(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attributeEnchantmentsApplyInMainhand(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyInMainhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attributeEnchantmentsApplyInOffhand(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyInOffhand(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attributeEnchantmentsApplyOnArmor(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyOnArmor(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attributeEnchantmentsApplyInCurios(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyInCurios(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void attributeEnchantmentsMergeWithExistingAttributes(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsMergeWithExistingAttributes(helper);
    }
}
