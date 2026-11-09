package jp.aquafactory.apprenticecodex.gametest.spell.projectile;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.STRAIGHT_PROJECTILE_COLLISION_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ProjectileCollisionGameTests {
    private ProjectileCollisionGameTests() {}

    @GameTest(template = TEMPLATE, batch = STRAIGHT_PROJECTILE_COLLISION_ISOLATED_BATCH, timeoutTicks = 40)
    public static void straightProjectilesTreatBoundingBoxGrazesAsBlockImpacts(GameTestHelper helper) {
        ForgeProjectileCollisionScenarios.straightProjectilesTreatBoundingBoxGrazesAsBlockImpacts(helper);
    }

    @GameTest(template = TEMPLATE, batch = STRAIGHT_PROJECTILE_COLLISION_ISOLATED_BATCH, timeoutTicks = 40)
    public static void straightProjectilesRespectCancelledBlockImpacts(GameTestHelper helper) {
        ForgeProjectileCollisionScenarios.straightProjectilesRespectCancelledBlockImpacts(helper);
    }
}
