package jp.aquafactory.apprenticecodex.gametest.spell.combustionjet;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.combustion_jet_isolated")
    public static void combustionJetCastSpawnsConfiguredWave(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetCastSpawnsConfiguredWave(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.combustion_jet_isolated")
    public static void combustionJetCastStopsBeforeThinCover(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetCastStopsBeforeThinCover(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.combustion_jet_isolated")
    public static void combustionJetWaveHitsWideAreaOnce(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetWaveHitsWideAreaOnce(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.combustion_jet_isolated")
    public static void combustionJetWaveAppliesEffectsWhenDamageFails(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetWaveAppliesEffectsWhenDamageFails(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.combustion_jet_isolated")
    public static void combustionJetWaveTargetsNonLivingCombatTarget(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetWaveTargetsNonLivingCombatTarget(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.combustion_jet_isolated")
    public static void combustionJetWaveUsesSmallBlockCollision(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetWaveUsesSmallBlockCollision(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.combustion_jet_isolated")
    public static void combustionJetWaveDisappearsAtRange(GameTestHelper helper) {
        CombustionJetGameTestScenarios.combustionJetWaveDisappearsAtRange(helper);
    }
}
