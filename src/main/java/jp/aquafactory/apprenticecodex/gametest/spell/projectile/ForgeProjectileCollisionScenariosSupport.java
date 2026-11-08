package jp.aquafactory.apprenticecodex.gametest.spell.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.spell.projectile.ForgeProjectileCollisionScenarios.spawnStraightProjectileCollisionTestSet;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class ForgeProjectileCollisionScenariosSupport {
    private ForgeProjectileCollisionScenariosSupport() {}

    static void straightProjectilesRespectNonDefaultBlockImpactResults(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "straight_projectile_impact_result_test");
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);

        var grazingStart = helper.absoluteVec(new Vec3(1.5D, 3.25D, 1.9D));
        var skipEntityProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, grazingStart);
        var stopAtCurrentProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, grazingStart);
        var stopWithoutDamageProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, grazingStart);
        var testedProjectiles = new LinkedHashSet<Projectile>();
        testedProjectiles.addAll(skipEntityProjectiles.values());
        testedProjectiles.addAll(stopAtCurrentProjectiles.values());
        testedProjectiles.addAll(stopWithoutDamageProjectiles.values());
        var impactCount = new AtomicInteger();
        Consumer<ProjectileImpactEvent> impactListener = event -> {
            if (!(event.getRayTraceResult() instanceof BlockHitResult)) {
                return;
            }
            if (skipEntityProjectiles.containsValue(event.getProjectile())) {
                impactCount.incrementAndGet();
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
            } else if (stopAtCurrentProjectiles.containsValue(event.getProjectile())) {
                impactCount.incrementAndGet();
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT);
            } else if (stopWithoutDamageProjectiles.containsValue(event.getProjectile())) {
                impactCount.incrementAndGet();
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            }
        };

        MinecraftForge.EVENT_BUS.addListener(impactListener);
        helper.runAtTickTime(2, () -> {
            try {
                helper.assertTrue(impactCount.get() >= testedProjectiles.size(),
                        "Each non-default block impact should fire ProjectileImpactEvent: "
                                + impactCount.get() + " / " + testedProjectiles.size());
                testedProjectiles.forEach(projectile -> helper.assertTrue(projectile.isRemoved(),
                        projectile.getType() + " should stop for a non-default block impact result"));
                helper.succeed();
            } finally {
                MinecraftForge.EVENT_BUS.unregister(impactListener);
            }
        });
    }
}
