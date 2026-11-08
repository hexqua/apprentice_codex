package jp.aquafactory.apprenticecodex.gametest.equipment.bow.elemental;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.ELEMENTAL_BOW_OVERHEAT_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ElementalBowSelectionGameTests {
    private ElementalBowSelectionGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void elementalBowBuildsSelectionViewsFromHeldAmmo(GameTestHelper helper) {
        ElementalBowSelectionScenarios.elementalBowBuildsSelectionViewsFromHeldAmmo(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowInventoryOverlayReflectsCurrentSelection(GameTestHelper helper) {
        ElementalBowSelectionScenarios.elementalBowInventoryOverlayReflectsCurrentSelection(helper);
    }

    @GameTest(template = TEMPLATE, batch = ELEMENTAL_BOW_OVERHEAT_BATCH, timeoutTicks = 80)
    public static void elementalBowSelectionViewExposesOverheatOverlayState(GameTestHelper helper) {
        ElementalBowSelectionScenarios.elementalBowSelectionViewExposesOverheatOverlayState(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(GameTestHelper helper) {
        ElementalBowSelectionScenarios.elementalBowKeepsCurrentEmptySpecialSelectionOnlyWhileSelected(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(GameTestHelper helper) {
        ElementalBowSelectionScenarios.elementalBowFallsBackToNoneWhenLegacyModeCannotResolve(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowUsesStoredScrollWithoutSpellContainer(GameTestHelper helper) {
        ElementalBowSelectionScenarios.elementalBowUsesStoredScrollWithoutSpellContainer(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(GameTestHelper helper) {
        ElementalBowSelectionScenarios.elementalBowDoesNotAddDerivedSpellToMainhandSpellWheel(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void elementalBowNonMagicModesHideDerivedSpellPresentation(GameTestHelper helper) {
        ElementalBowSelectionScenarios.elementalBowNonMagicModesHideDerivedSpellPresentation(helper);
    }
}
