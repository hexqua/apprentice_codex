package jp.aquafactory.apprenticecodex.gametest.spell.bloodbrand;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.BLOOD_BRAND_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = BLOOD_BRAND_ISOLATED_BATCH)
    public static void bloodBrandCastSpawnsOneGravityKunai(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandCastSpawnsOneGravityKunai(helper);
    }

    @GameTest(template = TEMPLATE, batch = BLOOD_BRAND_ISOLATED_BATCH)
    public static void bloodBrandKunaiMarksAndCleansState(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandKunaiMarksAndCleansState(helper);
    }

    @GameTest(template = TEMPLATE, batch = BLOOD_BRAND_ISOLATED_BATCH)
    public static void bloodBrandLethalKunaiBurstsAndHeals(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandLethalKunaiBurstsAndHeals(helper);
    }

    @GameTest(template = TEMPLATE, batch = BLOOD_BRAND_ISOLATED_BATCH)
    public static void bloodBrandBurstUsesSphereSightAndHalfHealing(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandBurstUsesSphereSightAndHalfHealing(helper);
    }

    @GameTest(template = TEMPLATE, batch = BLOOD_BRAND_ISOLATED_BATCH)
    public static void bloodBrandHiganbanaBurstEnhancesRangeDamageAndHealing(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandHiganbanaBurstEnhancesRangeDamageAndHealing(helper);
    }

    @GameTest(template = TEMPLATE, batch = BLOOD_BRAND_ISOLATED_BATCH)
    public static void bloodBrandWithoutCasterDoesNotBurst(GameTestHelper helper) {
        BloodBrandGameTestScenarios.bloodBrandWithoutCasterDoesNotBurst(helper);
    }
}
