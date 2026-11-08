package jp.aquafactory.apprenticecodex.gametest.enchantment.application;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EnchantmentApplicationGameTests {
    private EnchantmentApplicationGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void enchantmentApplicationSurfacesKeepExpectedMatrix(GameTestHelper helper) {
        EnchantmentApplicationGameTestScenarios.itemSurfacesKeepExpectedMatrix(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantmentDirectApplicationPoliciesKeepExpectedMatrix(GameTestHelper helper) {
        EnchantmentApplicationGameTestScenarios.directApplicationPoliciesKeepExpectedMatrix(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantmentSpecialApplicationRulesStayExplicit(GameTestHelper helper) {
        EnchantmentApplicationGameTestScenarios.specialApplicationRulesStayExplicit(helper);
    }
}
