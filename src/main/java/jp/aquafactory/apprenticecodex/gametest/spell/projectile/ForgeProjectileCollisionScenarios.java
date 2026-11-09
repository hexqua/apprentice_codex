package jp.aquafactory.apprenticecodex.gametest.spell.projectile;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeProjectileEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.bloodbrand.BloodBrandKunai;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceDaggerEntity;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.STRAIGHT_PROJECTILE_COLLISION_ISOLATED_BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.SpellTestBatches.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ForgeProjectileCollisionScenarios {
    private ForgeProjectileCollisionScenarios() {}

    static void straightProjectilesTreatBoundingBoxGrazesAsBlockImpacts(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "straight_projectile_collision_test");
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);

        // 中心はブロックの外側を通るが、幅 0.25 の当たり箱だけが z=2 の面を擦る軌道。
        var grazingStart = helper.absoluteVec(new Vec3(1.5D, 3.25D, 1.9D));
        var clearStart = helper.absoluteVec(new Vec3(1.5D, 3.25D, 4.5D));
        var grazingProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, grazingStart);
        var clearProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, clearStart);

        helper.runAtTickTime(2, () -> {
            grazingProjectiles.forEach((name, projectile) -> helper.assertTrue(projectile.isRemoved(),
                    name + " should treat a bounding-box block graze as an impact"));
            clearProjectiles.forEach((name, projectile) -> {
                helper.assertFalse(projectile.isRemoved(),
                        name + " should remain active without a physical collision");
                helper.assertTrue(projectile.position().distanceToSqr(clearStart) > 1.0D,
                        name + " should preserve unobstructed straight movement");
                projectile.discard();
            });
            helper.succeed();
        });
    }

    static void straightProjectilesRespectCancelledBlockImpacts(GameTestHelper helper) {
        var level = (ServerLevel) helper.getLevel();
        var caster = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0),
                "straight_projectile_cancelled_collision_test");
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);

        var grazingStart = helper.absoluteVec(new Vec3(1.5D, 3.25D, 1.9D));
        var centerHitStart = helper.absoluteVec(new Vec3(1.5D, 3.25D, 2.5D));
        var grazingProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, grazingStart);
        var centerHitProjectiles = spawnStraightProjectileCollisionTestSet(level, caster, centerHitStart);
        var cancelledProjectiles = new LinkedHashSet<Projectile>();
        cancelledProjectiles.addAll(grazingProjectiles.values());
        cancelledProjectiles.addAll(centerHitProjectiles.values());
        var impactCount = new AtomicInteger();
        Consumer<ProjectileImpactEvent> impactListener = event -> {
            if (cancelledProjectiles.contains(event.getProjectile())
                    && event.getRayTraceResult() instanceof BlockHitResult) {
                impactCount.incrementAndGet();
                // Forge 1.20.1 が維持する旧 MOD 向けの実キャンセル経路を検証する。
                ((Event) event).setCanceled(true);
            }
        };

        MinecraftForge.EVENT_BUS.addListener(impactListener);
        helper.runAtTickTime(2, () -> {
            try {
                helper.assertTrue(impactCount.get() >= cancelledProjectiles.size(),
                        "Each center or bounding-box block impact should fire ProjectileImpactEvent: "
                                + impactCount.get() + " / " + cancelledProjectiles.size());
                cancelledProjectiles.forEach(projectile -> {
                    helper.assertFalse(projectile.isRemoved(),
                            projectile.getType() + " should continue after a canceled block impact");
                    helper.assertTrue(projectile.getX() > grazingStart.x + 2.0D,
                            projectile.getType() + " should preserve movement after a canceled block impact");
                    helper.assertTrue(projectile.getDeltaMovement().x > 1.0D,
                            projectile.getType() + " should preserve velocity after a canceled block impact");
                    projectile.discard();
                });
                helper.succeed();
            } finally {
                MinecraftForge.EVENT_BUS.unregister(impactListener);
            }
        });
    }

    static Map<String, Projectile> spawnStraightProjectileCollisionTestSet(
            ServerLevel level,
            LivingEntity caster,
            Vec3 position
    ) {
        var direction = new Vec3(1.0D, 0.0D, 0.0D);
        var speed = 1.55D;
        var projectiles = new LinkedHashMap<String, Projectile>();

        var bloodBrand = new BloodBrandKunai(EntityRegistry.BLOOD_BRAND_KUNAI.get(), level, caster);
        bloodBrand.setProjectileVelocity(direction);
        projectiles.put("Blood Brand kunai", bloodBrand);

        var skyEdge = new SkyEdgeProjectileEntity(EntityRegistry.SKY_EDGE_PROJECTILE.get(), level, caster);
        skyEdge.setProjectileVelocity(direction, speed);
        projectiles.put("Sky Edge", skyEdge);

        var inscribeIce = new InscribeIceDaggerEntity(EntityRegistry.INSCRIBE_ICE_DAGGER.get(), level, caster);
        inscribeIce.setProjectileVelocity(direction, speed);
        projectiles.put("Inscribe Ice dagger", inscribeIce);

        var manaForceBlade = new ManaForceBladeProjectileEntity(
                EntityRegistry.MANA_FORCE_BLADE_PROJECTILE.get(), level, caster);
        manaForceBlade.setProjectileVelocity(direction, speed);
        projectiles.put("Mana Force Blade projectile", manaForceBlade);

        var mysticShield = new MysticShieldProjectileEntity(
                EntityRegistry.MYSTIC_SHIELD_PROJECTILE.get(), level, caster);
        mysticShield.shoot(direction);
        projectiles.put("Mystic Shield projectile", mysticShield);

        projectiles.values().forEach(projectile -> {
            projectile.setPos(position);
            level.addFreshEntity(projectile);
        });
        return projectiles;
    }

    @GameTest(template = TEMPLATE, batch = STRAIGHT_PROJECTILE_COLLISION_ISOLATED_BATCH, timeoutTicks = 40)
    public static void straightProjectilesRespectNonDefaultBlockImpactResults(GameTestHelper helper) {
        ForgeProjectileCollisionScenariosSupport.straightProjectilesRespectNonDefaultBlockImpactResults(helper);
    }
}
