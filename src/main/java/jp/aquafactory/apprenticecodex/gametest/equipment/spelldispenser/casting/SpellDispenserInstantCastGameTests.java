package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.casting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserInstantCastGameTests {
    private SpellDispenserInstantCastGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperCleansUpProxy(GameTestHelper helper) {
        SpellDispenserInstantCastScenarios.spellDispenserCastHelperCleansUpProxy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperAllowsOwnerOptionalSpellWithoutOwnerProfile(GameTestHelper helper) {
        SpellDispenserInstantCastScenarios.spellDispenserCastHelperAllowsOwnerOptionalSpellWithoutOwnerProfile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserWizardlampPlacesLanternInAirWithoutBlockTarget(GameTestHelper helper) {
        SpellDispenserInstantCastScenarios.spellDispenserWizardlampPlacesLanternInAirWithoutBlockTarget(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperUsesNeutralLivingCasterProfileForMagicMissile(GameTestHelper helper) {
        SpellDispenserInstantCastScenarios.spellDispenserCastHelperUsesNeutralLivingCasterProfileForMagicMissile(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperCompletesLongCastImmediately(GameTestHelper helper) {
        SpellDispenserInstantCastScenarios.spellDispenserCastHelperCompletesLongCastImmediately(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperTicksLongCastBeforeCompletion(GameTestHelper helper) {
        SpellDispenserInstantCastScenarios.spellDispenserCastHelperTicksLongCastBeforeCompletion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserPrecisionJackLowManaCleansUpKnife(GameTestHelper helper) {
        SpellDispenserInstantCastScenarios.spellDispenserPrecisionJackLowManaCleansUpKnife(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperSupportsDiagonalVectorFacing(GameTestHelper helper) {
        SpellDispenserInstantCastScenarios.spellDispenserCastHelperSupportsDiagonalVectorFacing(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserCastHelperSupportsSpectralHammer(GameTestHelper helper) {
        SpellDispenserInstantCastScenarios.spellDispenserCastHelperSupportsSpectralHammer(helper);
    }
}
