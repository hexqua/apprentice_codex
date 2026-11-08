package jp.aquafactory.apprenticecodex.gametest.spell.targeting.aim;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.BEAM_OCCLUSION_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BeamOcclusionGameTests {
    private BeamOcclusionGameTests() {}

    @GameTest(template = TEMPLATE, batch = BEAM_OCCLUSION_ISOLATED_BATCH)
    public static void beamLengthIgnoresNoCollisionGrass(GameTestHelper helper) {
        BeamOcclusionScenarios.beamLengthIgnoresNoCollisionGrass(helper);
    }
}
