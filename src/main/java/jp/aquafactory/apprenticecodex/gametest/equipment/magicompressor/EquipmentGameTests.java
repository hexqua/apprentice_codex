package jp.aquafactory.apprenticecodex.gametest.equipment.magicompressor;

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
    public static void magiCompressorGadgetUsesBeltSlotAndDedicatedImplementation(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetUsesBeltSlotAndDedicatedImplementation(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.magi_compressor_gadget_config")
    public static void magiCompressorGadgetCreateUnavailableDoesNotSpendManaOrStoreAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetCreateUnavailableDoesNotSpendManaOrStoreAir(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.magi_compressor_gadget_config")
    public static void magiCompressorGadgetConvertsConfiguredManaIntoAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetConvertsConfiguredManaIntoAir(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.magi_compressor_gadget_config")
    public static void magiCompressorGadgetConvertsProportionallyUpToMaxAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetConvertsProportionallyUpToMaxAir(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.magi_compressor_gadget_config")
    public static void magiCompressorGadgetInsufficientManaDoesNotStoreAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetInsufficientManaDoesNotStoreAir(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.magi_compressor_gadget_config")
    public static void magiCompressorGadgetBacktankSupplierClampsLegacyAirTag(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetBacktankSupplierClampsLegacyAirTag(helper);
    }
}
