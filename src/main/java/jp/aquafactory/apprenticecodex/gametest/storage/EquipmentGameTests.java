package jp.aquafactory.apprenticecodex.gametest.storage;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.LUMINOUS_DEVICE_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = TEMPLATE, batch = LUMINOUS_DEVICE_CONFIG_BATCH)
    public static void inventoryInsertHintsMatchStorageRules(GameTestHelper helper) {
        InventoryInsertHintGameTestScenarios.inventoryInsertHintsMatchStorageRules(helper);
    }
}
