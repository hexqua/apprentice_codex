package jp.aquafactory.apprenticecodex.gametest.spell.tinylumberjack;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.MINING_SPELL_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.equipment.EquipmentTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class TinyLumberjackDropGameTests {
    private TinyLumberjackDropGameTests() {}

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(GameTestHelper helper) {
        TinyLumberjackDropScenarios.tinyLumberjackWithCraftsmansDelightMovesJobDropsToOrigin(helper);
    }

    @GameTest(template = TEMPLATE, batch = MINING_SPELL_ISOLATED_BATCH)
    public static void tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(GameTestHelper helper) {
        TinyLumberjackDropScenarios.tinyLumberjackDropMoveFollowsCurrentCraftsmansDelightEquipment(helper);
    }
}
