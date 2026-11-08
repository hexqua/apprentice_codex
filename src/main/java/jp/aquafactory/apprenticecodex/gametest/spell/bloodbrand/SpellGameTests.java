package jp.aquafactory.apprenticecodex.gametest.spell.bloodbrand;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.blood_brand_isolated")
    public static void bloodBrandCastSpawnsOneGravityKunai(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandCastSpawnsOneGravityKunai(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.blood_brand_isolated")
    public static void bloodBrandKunaiMarksAndCleansState(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandKunaiMarksAndCleansState(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.blood_brand_isolated")
    public static void bloodBrandLethalKunaiBurstsAndHeals(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandLethalKunaiBurstsAndHeals(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.blood_brand_isolated")
    public static void bloodBrandBurstUsesSphereSightAndHalfHealing(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandBurstUsesSphereSightAndHalfHealing(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.blood_brand_isolated")
    public static void bloodBrandHiganbanaBurstEnhancesRangeDamageAndHealing(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandHiganbanaBurstEnhancesRangeDamageAndHealing(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.blood_brand_isolated")
    public static void bloodBrandWithoutCasterDoesNotBurst(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandWithoutCasterDoesNotBurst(helper);
    }
}
