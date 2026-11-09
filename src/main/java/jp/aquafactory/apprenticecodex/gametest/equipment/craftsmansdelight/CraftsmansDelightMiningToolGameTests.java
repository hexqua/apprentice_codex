package jp.aquafactory.apprenticecodex.gametest.equipment.craftsmansdelight;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.MINING_SPELL_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CraftsmansDelightMiningToolGameTests {
    private CraftsmansDelightMiningToolGameTests() {}

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void craftsmansDelightDummyToolsCopyAllCurrentMainHandEnchantments(GameTestHelper helper) {
        CraftsmansDelightLegacyScenarios.craftsmansDelightDummyToolsCopyAllCurrentMainHandEnchantments(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void spectralHammerCopiesCurrentMainHandWithCraftsmansDelight(GameTestHelper helper) {
        CraftsmansDelightMiningToolScenarios.spectralHammerCopiesCurrentMainHandWithCraftsmansDelight(helper);
    }
}
