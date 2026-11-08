package jp.aquafactory.apprenticecodex.gametest.spell.touchdig;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.craftsmansdelight.CraftsmansDelightLegacyScenarios;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.MINING_SPELL_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class TouchDigHarvestGameTests {
    private TouchDigHarvestGameTests() {}

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void craftsmansDelightExtendsTouchDigRange(GameTestHelper helper) {
        CraftsmansDelightLegacyScenarios.craftsmansDelightExtendsTouchDigRange(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void touchDigIgnoresCraftsmansDelightRingEnchantments(GameTestHelper helper) {
        TouchDigHarvestScenarios.touchDigIgnoresCraftsmansDelightRingEnchantments(helper);
    }
}
