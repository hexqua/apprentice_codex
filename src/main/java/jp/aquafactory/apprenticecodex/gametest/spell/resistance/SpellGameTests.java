package jp.aquafactory.apprenticecodex.gametest.spell.resistance;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.fire_spell_resistance_isolated")
    public static void thermalProcessAndCombustionJetDamageFireResistantTargets(GameTestHelper helper) {
        FireSpellResistanceGameTestScenarios.thermalProcessAndCombustionJetDamageFireResistantTargets(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.fire_spell_resistance_isolated")
    public static void thermalProcessSkipsEffectsForFireResistantTargets(GameTestHelper helper) {
        FireSpellResistanceGameTestScenarios.thermalProcessSkipsEffectsForFireResistantTargets(helper);
    }
}
