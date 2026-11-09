package jp.aquafactory.apprenticecodex.gametest.spell.combustionjet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.COMBUSTION_JET_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = COMBUSTION_JET_ISOLATED_BATCH)
    public static void combustionJetCastSpawnsConfiguredWave(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetCastSpawnsConfiguredWave(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMBUSTION_JET_ISOLATED_BATCH)
    public static void combustionJetCastStopsBeforeThinCover(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetCastStopsBeforeThinCover(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMBUSTION_JET_ISOLATED_BATCH)
    public static void combustionJetWaveHitsWideAreaOnce(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetWaveHitsWideAreaOnce(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMBUSTION_JET_ISOLATED_BATCH)
    public static void combustionJetWaveAppliesEffectsWhenDamageFails(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetWaveAppliesEffectsWhenDamageFails(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMBUSTION_JET_ISOLATED_BATCH)
    public static void combustionJetWaveTargetsNonLivingCombatTarget(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetWaveTargetsNonLivingCombatTarget(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMBUSTION_JET_ISOLATED_BATCH)
    public static void combustionJetWaveUsesSmallBlockCollision(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetWaveUsesSmallBlockCollision(helper);
    }

    @GameTest(template = TEMPLATE, batch = COMBUSTION_JET_ISOLATED_BATCH)
    public static void combustionJetWaveDisappearsAtRange(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetWaveDisappearsAtRange(helper);
    }
}
