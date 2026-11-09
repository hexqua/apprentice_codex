package jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScrollcasterEquipmentGameTests {
    private ScrollcasterEquipmentGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletKeepsExpectedStatsAndBenchRules(GameTestHelper helper) {
        ScrollcasterEquipmentScenarios.scrollcasterGauntletKeepsExpectedStatsAndBenchRules(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(GameTestHelper helper) {
        ScrollcasterEquipmentGameTestScenarios.scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
        ScrollcasterEquipmentGameTestScenarios.scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(helper);
    }
}
