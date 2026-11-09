package jp.aquafactory.apprenticecodex.gametest.spell.resistance;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.FIRE_SPELL_RESISTANCE_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = TEMPLATE, batch = FIRE_SPELL_RESISTANCE_ISOLATED_BATCH)
    public static void thermalProcessAndCombustionJetDamageFireResistantTargets(GameTestHelper helper) {
        FireSpellResistanceGameTestScenarios.thermalProcessAndCombustionJetDamageFireResistantTargets(helper);
    }

    @GameTest(template = TEMPLATE, batch = FIRE_SPELL_RESISTANCE_ISOLATED_BATCH)
    public static void thermalProcessSkipsEffectsForFireResistantTargets(GameTestHelper helper) {
        FireSpellResistanceGameTestScenarios.thermalProcessSkipsEffectsForFireResistantTargets(helper);
    }
}
