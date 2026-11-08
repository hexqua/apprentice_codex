package jp.aquafactory.apprenticecodex.gametest.effect.castingmobility;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CastingMobilityGameTests {
    private CastingMobilityGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(GameTestHelper helper) {
        CastingMobilityScenarios.castingMoveSpeedAdjustmentStopsAtNormalSpeedWithoutNegativeCorrections(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(GameTestHelper helper) {
        CastingMobilityScenarios.dynamicCastingMobilityEffectRebalancesAgainstExternalCastingMoveSpeed(helper);
    }
}
