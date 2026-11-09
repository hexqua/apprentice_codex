package jp.aquafactory.apprenticecodex.gametest.enchantment.attribute;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class AttributeEnchantmentGameTests {
    private AttributeEnchantmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void forcedAttributeEnchantmentsCoexistAndKeepRawPositiveLevels(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.forcedEnchantmentsCoexistAndKeepRawPositiveLevels(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void attunementResolutionFailureIsNoOp(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.attunementResolutionFailureIsNoOp(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magicAttributeMergeOnlyNormalizesLinearMagicAttributes(GameTestHelper helper) {
        AttributeEnchantmentPolicyGameTestScenarios.mergeHelperOnlyNormalizesLinearMagicAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void attributeEnchantmentsApplyInMainhand(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyInMainhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void attributeEnchantmentsApplyInOffhand(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyInOffhand(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void attributeEnchantmentsApplyOnArmor(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyOnArmor(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void attributeEnchantmentsApplyInCurios(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsApplyInCurios(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void attributeEnchantmentsMergeWithExistingAttributes(GameTestHelper helper) {
        AttributeEnchantmentEffectGameTestScenarios.attributeEnchantmentsMergeWithExistingAttributes(helper);
    }
}
