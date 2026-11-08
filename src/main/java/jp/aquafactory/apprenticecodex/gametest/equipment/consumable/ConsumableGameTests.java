package jp.aquafactory.apprenticecodex.gametest.equipment.consumable;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ConsumableGameTests {
    private ConsumableGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void comfortBerriesProvideManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        ConsumableScenarios.comfortBerriesProvideManaRegenerationAndExpectedFoodValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void comfortSandwichProvidesManaRegenerationAndExpectedFoodValues(GameTestHelper helper) {
        ConsumableScenarios.comfortSandwichProvidesManaRegenerationAndExpectedFoodValues(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void meditationPotionsExposeExpectedEffectsAndDurations(GameTestHelper helper) {
        ConsumableScenarios.meditationPotionsExposeExpectedEffectsAndDurations(helper);
    }
}
