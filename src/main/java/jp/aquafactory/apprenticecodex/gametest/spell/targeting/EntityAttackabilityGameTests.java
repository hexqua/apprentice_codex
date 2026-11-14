package jp.aquafactory.apprenticecodex.gametest.spell.targeting;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EntityAttackabilityGameTests {
    private EntityAttackabilityGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void magitechIgnoresConstructsWithoutLosingCombatTargets(GameTestHelper helper) {
        if (ModList.get().isLoaded("magitech")) MagitechTargetingScenarios.mining(helper);
        else helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void redirectableTagControlsPlayerProjectileDeflection(GameTestHelper helper) {
        ProjectileAttackabilityScenarios.deflection(helper);
    }
}
