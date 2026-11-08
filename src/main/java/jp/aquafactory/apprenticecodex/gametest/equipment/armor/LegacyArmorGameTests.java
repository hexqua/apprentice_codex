package jp.aquafactory.apprenticecodex.gametest.equipment.armor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.menu.CalibrationImbueGameTests;
import jp.aquafactory.apprenticecodex.gametest.equipment.offhand.OffhandAndBetterCombatGameTestScenarios;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LegacyArmorGameTests {
    private LegacyArmorGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void endgameArmorCalibrationAppliesSharedRulesAndAttributes(GameTestHelper helper) {
        CalibrationImbueGameTests.endgameArmorCalibrationAppliesSharedRulesAndAttributes(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void endgameArmorScrollwovenSlotsPersistAndFollowSelectionOrder(GameTestHelper helper) {
        CalibrationImbueGameTests.endgameArmorScrollwovenSlotsPersistAndFollowSelectionOrder(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.diamondAndNetheriteSpellAmplifierExposeNewAttributeBonuses(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedCircletCurioModifiersStayIndependentAcrossSlots(GameTestHelper helper) {
        OffhandAndBetterCombatGameTestScenarios.enchantedCircletCurioModifiersStayIndependentAcrossSlots(helper);
    }
}
