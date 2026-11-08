package jp.aquafactory.apprenticecodex.gametest.spell.manamending;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.runtime.CoreGameTests.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void manaMendingRequiresDamagedHeldItem(GameTestHelper helper) {
        ManaMendingScenarios.manaMendingRequiresDamagedHeldItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaMendingRepairsMainHandDamagedItem(GameTestHelper helper) {
        ManaMendingScenarios.manaMendingRepairsMainHandDamagedItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaMendingPrefersOffhandDamagedItem(GameTestHelper helper) {
        ManaMendingScenarios.manaMendingPrefersOffhandDamagedItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void manaMendingCraftsmansDelightBoostsRepairAndPreservesRepairCost(GameTestHelper helper) {
        ManaMendingScenarios.manaMendingCraftsmansDelightBoostsRepairAndPreservesRepairCost(helper);
    }
}
