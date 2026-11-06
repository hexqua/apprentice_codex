package jp.aquafactory.apprenticecodex.gametest.spell.manamending;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class CoreGameTests {
    private CoreGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void manaMendingRequiresDamagedHeldItem(GameTestHelper helper) {
        ManaMendingScenarios.manaMendingRequiresDamagedHeldItem(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaMendingRepairsMainHandDamagedItem(GameTestHelper helper) {
        ManaMendingScenarios.manaMendingRepairsMainHandDamagedItem(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaMendingPrefersOffhandDamagedItem(GameTestHelper helper) {
        ManaMendingScenarios.manaMendingPrefersOffhandDamagedItem(helper);
    }

    @GameTest(template = "gametest/basic_floor")
    public static void manaMendingCraftsmansDelightBoostsRepairAndPreservesRepairCost(GameTestHelper helper) {
        ManaMendingScenarios.manaMendingCraftsmansDelightBoostsRepairAndPreservesRepairCost(helper);
    }
}
