package jp.aquafactory.apprenticecodex.gametest.equipment.armor;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MagicArmorGameTests {
    private MagicArmorGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void apprenticeMageRobeKeepsExpectedAttributeBonuses(GameTestHelper helper) {
        MagicArmorGameTestScenarios.apprenticeMageRobeKeepsExpectedAttributeBonuses(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        MagicArmorGameTestScenarios.enchantressRobeKeepsExpectedAttributeBonusesAndImbueSurface(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(GameTestHelper helper) {
        MagicArmorGameTestScenarios.enchantressRobeChestplateAddsImbuedSchoolSpellPowerWithoutChangingGlobalSpellPower(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(GameTestHelper helper) {
        MagicArmorGameTestScenarios.chromaticMagiaDressKeepsExpectedStatsAndImbueSurface(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void magiAgentSuitKeepsExpectedStatsImbueAndCalibrationRune(GameTestHelper helper) {
        MagicArmorGameTestScenarios.magiAgentSuitKeepsExpectedStatsImbueAndCalibrationRune(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(GameTestHelper helper) {
        MagicArmorGameTestScenarios.stealthRuneArmorKeepsExpectedAttributeBonusesAndImbueSurface(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void soulcollectorRobeAddsLodestoneMagicProficiencyAndMalumInfusions(GameTestHelper helper) {
        MagicArmorGameTestScenarios.soulcollectorRobeAddsLodestoneMagicProficiencyAndMalumInfusions(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(GameTestHelper helper) {
        MagicArmorGameTestScenarios.chromaticMagiaDressRecordsCastHistoryByArmorTypeAndIgnoresRecasts(helper);
    }
}
