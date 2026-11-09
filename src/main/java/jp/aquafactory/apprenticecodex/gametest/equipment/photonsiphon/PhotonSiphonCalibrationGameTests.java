package jp.aquafactory.apprenticecodex.gametest.equipment.photonsiphon;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class PhotonSiphonCalibrationGameTests {
    private PhotonSiphonCalibrationGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void photonSiphonStartsWithLockedManaChargeAndIsNotUnique(GameTestHelper helper) {
        PhotonSiphonCalibrationScenarios.photonSiphonStartsWithLockedManaChargeAndIsNotUnique(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void photonSiphonCalibrationRepairUnlocksLegacyReplacementOnly(GameTestHelper helper) {
        PhotonSiphonCalibrationScenarios.photonSiphonCalibrationRepairUnlocksLegacyReplacementOnly(helper);
    }
}
