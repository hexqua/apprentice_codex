package jp.aquafactory.apprenticecodex.gametest.spell.autoturret;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.AUTO_TURRET_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class AutoTurretRestockGameTests {
    private AutoTurretRestockGameTests() {}

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH)
    public static void autoTurretRestockConsumesManaAndRestoresAmmo(GameTestHelper helper) {
        AutoTurretRestockScenarios.autoTurretRestockConsumesManaAndRestoresAmmo(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH)
    public static void autoTurretRestockFullAmmoDoesNotSpendMana(GameTestHelper helper) {
        AutoTurretRestockScenarios.autoTurretRestockFullAmmoDoesNotSpendMana(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH)
    public static void autoTurretRestockInsufficientManaDoesNotRestoreAmmo(GameTestHelper helper) {
        AutoTurretRestockScenarios.autoTurretRestockInsufficientManaDoesNotRestoreAmmo(helper);
    }

    @GameTest(template = TEMPLATE, batch = AUTO_TURRET_ISOLATED_BATCH, timeoutTicks = 180)
    public static void autoTurretAmmoDepletionKeepsAliveAndRestockClearsDiscardDelay(GameTestHelper helper) {
        AutoTurretRestockScenarios.autoTurretAmmoDepletionKeepsAliveAndRestockClearsDiscardDelay(helper);
    }
}
