package jp.aquafactory.apprenticecodex.gametest.spell.autoturret;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_turret_isolated")
    public static void autoTurretRestockConsumesManaAndRestoresAmmo(GameTestHelper helper) {
        AutoTurretScenarios.autoTurretRestockConsumesManaAndRestoresAmmo(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_turret_isolated")
    public static void autoTurretCanBePlacedOnSupportedSlab(GameTestHelper helper) {
        AutoTurretScenarios.autoTurretCanBePlacedOnSupportedSlab(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_turret_isolated")
    public static void autoTurretCanBePlacedOnSupportedStairs(GameTestHelper helper) {
        AutoTurretScenarios.autoTurretCanBePlacedOnSupportedStairs(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_turret_isolated", timeoutTicks = 80)
    public static void autoTurretFallsWhenSupportRemoved(GameTestHelper helper) {
        AutoTurretScenarios.autoTurretFallsWhenSupportRemoved(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_turret_isolated")
    public static void autoTurretRestockFullAmmoDoesNotSpendMana(GameTestHelper helper) {
        AutoTurretScenarios.autoTurretRestockFullAmmoDoesNotSpendMana(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_turret_isolated")
    public static void autoTurretRestockInsufficientManaDoesNotRestoreAmmo(GameTestHelper helper) {
        AutoTurretScenarios.autoTurretRestockInsufficientManaDoesNotRestoreAmmo(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.auto_turret_isolated", timeoutTicks = 180)
    public static void autoTurretAmmoDepletionKeepsAliveAndRestockClearsDiscardDelay(GameTestHelper helper) {
        AutoTurretScenarios.autoTurretAmmoDepletionKeepsAliveAndRestockClearsDiscardDelay(helper);
    }
}
