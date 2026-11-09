package jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MonarchBondRestockGameTests {
    private MonarchBondRestockGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void monarchBondAutoRestockRefillsEmptyOwnedTurretAtRangeBoundary(GameTestHelper helper) {
        MonarchBondTestSupport.autoRestockRefillsEmptyOwnedTurretAtRangeBoundary(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void monarchBondAutoRestockUsesNearestDeterministicPriority(GameTestHelper helper) {
        MonarchBondTestSupport.autoRestockUsesNearestDeterministicPriority(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void monarchBondAutoRestockSkipsIneligibleAndUnaffordableTurrets(GameTestHelper helper) {
        MonarchBondTestSupport.autoRestockSkipsIneligibleAndUnaffordableTurrets(helper);
    }
}
