package jp.aquafactory.apprenticecodex.gametest.spell.projectile;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceDaggerEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static jp.aquafactory.apprenticecodex.gametest.spell.projectile.ProjectileCollisionFixture.BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.projectile.ProjectileCollisionFixture.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ProjectileCollisionGameTests {
    private ProjectileCollisionGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void straightProjectilesTreatBoundingBoxGrazesAsBlockImpacts(GameTestHelper helper) {
        var scene = new ProjectileCollisionFixture(helper);
        // 中心は壁の外を通り、幅0.25のAABBだけが接触する位置を維持する。
        var grazing = scene.projectiles(scene.position(new Vec3(1.5, 3.25, 1.9)));
        var clearStart = scene.position(new Vec3(1.5, 3.25, 4.5));
        var clear = scene.projectiles(clearStart);
        var grazingImpacts = scene.observe(grazing, false);
        var clearImpacts = scene.observe(clear, false);
        helper.runAtTickTime(2, () -> {
            grazing.forEach((name, projectile) -> {
                if (!(projectile instanceof InscribeIceDaggerEntity)) {
                    helper.assertTrue(projectile.isRemoved(), name + " must stop after a bounding-box graze: " + grazingImpacts.diagnostics(projectile));
                    grazingImpacts.assertHit(name, projectile);
                }
            });
            clear.forEach((name, projectile) -> {
                helper.assertTrue(!projectile.isRemoved() && clearImpacts.count(projectile) == 0
                                && projectile.position().distanceToSqr(clearStart) > 1,
                        name + " must move without a block impact on the clear lane: " + clearImpacts.diagnostics(projectile));
                scene.scope.assertVisible(projectile);
                projectile.discard();
            });
        });
        // Inscribe Iceの地形衝突猶予が終わるまで元と同じ6tickを観測する。
        helper.runAtTickTime(6, () -> {
            var dagger = grazing.get("Inscribe Ice dagger");
            helper.assertTrue(dagger.isRemoved(), "Inscribe Ice must impact after its grace period: " + grazingImpacts.diagnostics(dagger));
            grazingImpacts.assertHit("Inscribe Ice dagger", dagger);
            scene.close();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void straightProjectilesRespectCancelledBlockImpacts(GameTestHelper helper) {
        var scene = new ProjectileCollisionFixture(helper);
        var start = scene.position(new Vec3(1.5, 3.25, 1.9));
        var shots = new LinkedHashMap<String, Projectile>();
        scene.projectiles(start).forEach((name, projectile) -> shots.put(name + " graze", projectile));
        scene.projectiles(scene.position(new Vec3(1.5, 3.25, 2.5))).forEach((name, projectile) -> shots.put(name + " center", projectile));
        var impacts = scene.observe(shots, true);
        helper.runAtTickTime(2, () -> {
            shots.forEach((name, projectile) -> {
                impacts.assertHit(name, projectile);
                helper.assertTrue(!projectile.isRemoved() && projectile.getX() > start.x + 2 && projectile.getDeltaMovement().x > 1,
                        name + " must preserve movement and velocity after cancellation: " + impacts.diagnostics(projectile));
                scene.scope.assertVisible(projectile);
            });
            scene.close();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 40)
    public static void inscribeIceGraceDoesNotDuplicateBlockImpactEvent(GameTestHelper helper) {
        try (var scene = new ProjectileCollisionFixture(helper)) {
            var dagger = new InscribeIceDaggerEntity(EntityRegistry.INSCRIBE_ICE_DAGGER.get(), scene.level, scene.caster);
            dagger.setPos(scene.position(new Vec3(1.5, 3.25, 2.5)));
            dagger.setProjectileVelocity(new Vec3(1, 0, 0), 1.55);
            scene.scope.add(dagger);
            var impacts = scene.observe(Map.of("Inscribe Ice dagger", dagger), false);
            scene.scope.step(dagger, false);
            helper.assertTrue(!dagger.isRemoved() && impacts.count(dagger) == 1,
                    "A single contact during grace must post exactly one event without removal: " + impacts.diagnostics(dagger));
            impacts.assertHit("Inscribe Ice dagger", dagger);
        }
        helper.succeed();
    }
}
