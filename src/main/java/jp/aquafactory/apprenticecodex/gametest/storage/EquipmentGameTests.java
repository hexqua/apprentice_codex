package jp.aquafactory.apprenticecodex.gametest.storage;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EquipmentGameTests {
    private EquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.luminous_device_config")
    public static void inventoryInsertHintsMatchStorageRules(GameTestHelper helper) {
        InventoryInsertHintGameTestScenarios.inventoryInsertHintsMatchStorageRules(helper);
    }
}
