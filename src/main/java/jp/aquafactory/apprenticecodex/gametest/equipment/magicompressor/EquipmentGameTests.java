package jp.aquafactory.apprenticecodex.gametest.equipment.magicompressor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.MAGI_COMPRESSOR_GADGET_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void magiCompressorGadgetUsesBeltSlotAndDedicatedImplementation(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetUsesBeltSlotAndDedicatedImplementation(helper);
    }

    @GameTest(template = TEMPLATE, batch = MAGI_COMPRESSOR_GADGET_CONFIG_BATCH)
    public static void magiCompressorGadgetCreateUnavailableDoesNotSpendManaOrStoreAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetCreateUnavailableDoesNotSpendManaOrStoreAir(helper);
    }

    @GameTest(template = TEMPLATE, batch = MAGI_COMPRESSOR_GADGET_CONFIG_BATCH)
    public static void magiCompressorGadgetConvertsConfiguredManaIntoAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetConvertsConfiguredManaIntoAir(helper);
    }

    @GameTest(template = TEMPLATE, batch = MAGI_COMPRESSOR_GADGET_CONFIG_BATCH)
    public static void magiCompressorGadgetConvertsProportionallyUpToMaxAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetConvertsProportionallyUpToMaxAir(helper);
    }

    @GameTest(template = TEMPLATE, batch = MAGI_COMPRESSOR_GADGET_CONFIG_BATCH)
    public static void magiCompressorGadgetInsufficientManaDoesNotStoreAir(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetInsufficientManaDoesNotStoreAir(helper);
    }

    @GameTest(template = TEMPLATE, batch = MAGI_COMPRESSOR_GADGET_CONFIG_BATCH)
    public static void magiCompressorGadgetBacktankSupplierClampsLegacyAirTag(GameTestHelper helper) {
        MagiCompressorGadgetGameTestScenarios.magiCompressorGadgetBacktankSupplierClampsLegacyAirTag(helper);
    }
}
