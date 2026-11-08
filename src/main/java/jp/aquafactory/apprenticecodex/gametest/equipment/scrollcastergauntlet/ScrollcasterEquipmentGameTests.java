package jp.aquafactory.apprenticecodex.gametest.equipment.scrollcastergauntlet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ScrollcasterEquipmentGameTests {
    private ScrollcasterEquipmentGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletKeepsExpectedStatsAndBenchRules(GameTestHelper helper) {
        ScrollcasterEquipmentGameTestScenarios.scrollcasterGauntletKeepsExpectedStatsAndBenchEnchantingRules(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(GameTestHelper helper) {
        ScrollcasterEquipmentGameTestScenarios.scrollcasterGauntletOffhandUseCastsSelectedScrollWhenMainHandDoesNotConsumeUse(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(GameTestHelper helper) {
        ScrollcasterEquipmentGameTestScenarios.scrollcasterGauntletMainhandPrioritizesSupportedOffhandUseItems(helper);
    }
}
