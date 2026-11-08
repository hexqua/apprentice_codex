package jp.aquafactory.apprenticecodex.gametest.equipment.monarchbond;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MonarchBondHealingGameTests {
    private MonarchBondHealingGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void monarchBondSpellHealOverflowIsReservedNearestFirst(GameTestHelper helper) {
        MonarchBondTestSupport.spellHealOverflowIsReservedNearestFirst(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void monarchBondSpellHealRequiresEquippedWearerAndMatchingOwner(GameTestHelper helper) {
        MonarchBondTestSupport.spellHealRequiresEquippedWearerAndMatchingOwner(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void monarchBondGreaterHealFullyRestoresEveryOwnedTarget(GameTestHelper helper) {
        MonarchBondTestSupport.greaterHealFullyRestoresEveryOwnedTarget(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void monarchBondSpellLifestealDoesNotDependOnSchool(GameTestHelper helper) {
        MonarchBondTestSupport.spellLifestealDoesNotDependOnSchool(helper);
    }
}
