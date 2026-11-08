package jp.aquafactory.apprenticecodex.gametest.enchantment.acquisition;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.enchantment.application.EnchantmentApplicationGameTestScenarios;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EnchantmentAcquisitionGameTests {
    private EnchantmentAcquisitionGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void apprenticeEnchantmentAcquisitionFlagsKeepExpectedValues(GameTestHelper helper) {
        EnchantmentApplicationGameTestScenarios.acquisitionFlagsKeepExpectedValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void randomApplicableBookEnchantmentsExcludeFlaskEnchantments(GameTestHelper helper) {
        EnchantmentAcquisitionScenarios.randomApplicableBookEnchantmentsExcludeFlaskEnchantments(helper);
    }
}
