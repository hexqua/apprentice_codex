package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowUseGameTests {
    private ElementalBowUseGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void elementalBowRequiresManaBeforeStartingElementalDraw(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowRequiresManaBeforeStartingElementalDraw(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowManaErrorUsesIronsSpellbooksTranslationKey(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowManaErrorUsesIronsSpellbooksTranslationKey(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowDoesNotConsumeResourcesBeforeFullDraw(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowDoesNotConsumeResourcesBeforeFullDraw(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowDoesNotCastWhenArrowDisappearsDuringDraw(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowDoesNotCastWhenArrowDisappearsDuringDraw(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowInfinityAllowsVanillaDrawWithoutArrows(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowInfinityAllowsVanillaDrawWithoutArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowVanillaModeConsumesSpecialArrowWhenNormalArrowsAreMissing(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowArrowModeRequiresNormalArrowsEvenWhenSpecialArrowsExist(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowInfinityAllowsArrowModeDrawWithoutArrows(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowInfinityAllowsArrowModeDrawWithoutArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowMagicModeIgnoresInfinityWithoutAmmo(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowMagicModeIgnoresInfinityWithoutAmmo(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSynthesisAllowsMagicModeWithoutArrows(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowSynthesisAllowsMagicModeWithoutArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowCreativeAllowsMagicModeWithoutArrows(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowCreativeAllowsMagicModeWithoutArrows(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowMagicArrowCatalystItemsAllowsConfiguredSpecialArrow(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowMagicArrowCatalystItemsAllowsConfiguredSpecialArrow(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowSynthesisDoesNotConsumeMagicModeArrows(GameTestHelper helper) {
        ElementalBowUseScenarios.elementalBowSynthesisDoesNotConsumeMagicModeArrows(helper);
    }
}
