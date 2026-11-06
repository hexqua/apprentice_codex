package jp.aquafactory.apprenticecodex.gametest.spell.projectile;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellGameTests {
    private SpellGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.straight_projectile_collision_isolated", timeoutTicks = 40)
    public static void straightProjectilesTreatBoundingBoxGrazesAsBlockImpacts(GameTestHelper helper) {
        ProjectileCollisionScenarios.straightProjectilesTreatBoundingBoxGrazesAsBlockImpacts(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.straight_projectile_collision_isolated", timeoutTicks = 40)
    public static void straightProjectilesRespectCancelledBlockImpacts(GameTestHelper helper) {
        ProjectileCollisionScenarios.straightProjectilesRespectCancelledBlockImpacts(helper);
    }

    @GameTest(template = "gametest/basic_floor", batch = "apprenticecodex.straight_projectile_collision_isolated", timeoutTicks = 40)
    public static void inscribeIceGraceDoesNotDuplicateBlockImpactEvent(GameTestHelper helper) {
        ProjectileCollisionScenarios.inscribeIceGraceDoesNotDuplicateBlockImpactEvent(helper);
    }
}
