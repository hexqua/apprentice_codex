package jp.aquafactory.apprenticecodex.gametest.equipment.spellsideedge;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellSideEdgeLegacyScenarios {
    private SpellSideEdgeLegacyScenarios() {}

    @GameTest(template = TEMPLATE)
    public static void spellSideEdgeStartsWithEdgeDancerAndExpectedStats(GameTestHelper helper) {
        SpellSideEdgeGameTestScenarios.spellSideEdgeStartsWithEdgeDancerAndExpectedStats(helper);
    }
}
