package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowContractGameTests {
    private ElementalBowContractGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void elementalBowHeldWisdomAndPlunderWorkInBothHands(GameTestHelper helper) {
        ElementalBowLegacyScenarios.elementalBowHeldWisdomAndPlunderWorkInBothHands(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowInsufficientArrowErrorUsesDedicatedTranslationKey(GameTestHelper helper) {
        ElementalBowLegacyScenarios.elementalBowInsufficientArrowErrorUsesDedicatedTranslationKey(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSpecialModeInfinityKeepsSelectionAndAllowsEmptyReuse(GameTestHelper helper) {
        ElementalBowLegacyScenarios.elementalBowSpecialModeInfinityKeepsSelectionAndAllowsEmptyReuse(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSynthesisTooltipKeepsExpectedState(GameTestHelper helper) {
        ElementalBowLegacyScenarios.elementalBowSynthesisTooltipKeepsExpectedState(helper);
    }
}
