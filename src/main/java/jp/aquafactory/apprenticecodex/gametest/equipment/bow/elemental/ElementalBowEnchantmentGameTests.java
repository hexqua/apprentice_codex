package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.ELEMENTAL_BOW_POWER_CONFIG_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowEnchantmentGameTests {
    private ElementalBowEnchantmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void elementalBowScrollLevelUsesOnlyTranscendence(GameTestHelper helper) {
        ElementalBowEnchantmentScenarios.elementalBowScrollLevelUsesOnlyTranscendence(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowBlocksArcaneAnvilImbueViaSpellValidator(GameTestHelper helper) {
        ElementalBowEnchantmentScenarios.elementalBowBlocksArcaneAnvilImbueViaSpellValidator(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_POWER_CONFIG_BATCH)
    public static void elementalBowPowerDoesNotIncreaseSpellLevel(GameTestHelper helper) {
        ElementalBowEnchantmentScenarios.elementalBowPowerDoesNotIncreaseSpellLevel(helper);
    }
}
