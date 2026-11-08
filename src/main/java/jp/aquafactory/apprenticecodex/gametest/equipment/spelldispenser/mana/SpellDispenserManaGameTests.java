package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.mana;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.SpellDispenserGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserManaGameTests {
    private SpellDispenserManaGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidAcceptsOnlyRegularManaPotions(GameTestHelper helper) {
        SpellDispenserManaScenarios.spellDispenserFluidAcceptsOnlyRegularManaPotions(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidConsumesPotionDoseImmediately(GameTestHelper helper) {
        SpellDispenserManaScenarios.spellDispenserFluidConsumesPotionDoseImmediately(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidDrainsUnconsumedPotionOnly(GameTestHelper helper) {
        SpellDispenserManaScenarios.spellDispenserFluidDrainsUnconsumedPotionOnly(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserFluidPersistsThroughNbt(GameTestHelper helper) {
        SpellDispenserManaScenarios.spellDispenserFluidPersistsThroughNbt(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserComparatorOutputMatchesStoredMana(GameTestHelper helper) {
        SpellDispenserManaScenarios.spellDispenserComparatorOutputMatchesStoredMana(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserRefillsFromHighestManaPotionThatFits(GameTestHelper helper) {
        SpellDispenserManaScenarios.spellDispenserRefillsFromHighestManaPotionThatFits(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void spellDispenserRefillsFromGlowEnergyFlask(GameTestHelper helper) {
        SpellDispenserManaScenarios.spellDispenserRefillsFromGlowEnergyFlask(helper);
    }
}
