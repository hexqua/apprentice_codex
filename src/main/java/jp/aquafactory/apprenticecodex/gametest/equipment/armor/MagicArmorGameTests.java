package jp.aquafactory.apprenticecodex.gametest.equipment.armor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MagicArmorGameTests {
    private MagicArmorGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void apprenticeMageRobeKeepsExpectedAttributeBonuses(GameTestHelper helper) {
        MagicArmorGameTestScenarios.apprenticeMageRobeKeepsExpectedAttributeBonuses(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        MagicArmorGameTestScenarios.enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
        MagicArmorGameTestScenarios.enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        MagicArmorGameTestScenarios.chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void magiAgentSuitKeepsExpectedStatsImbueAndCalibrationRune(GameTestHelper helper) {
        MagicArmorGameTestScenarios.magiAgentSuitKeepsExpectedStatsImbueAndCalibrationRune(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        MagicArmorGameTestScenarios.stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void soulcollectorRobeAddsLodestoneMagicProficiencyAndMalumInfusions(GameTestHelper helper) {
        MagicArmorGameTestScenarios.soulcollectorRobeAddsLodestoneMagicProficiencyAndMalumInfusions(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(GameTestHelper helper) {
        MagicArmorGameTestScenarios.chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(helper);
    }
}
