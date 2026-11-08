package jp.aquafactory.apprenticecodex.gametest.spell.projectile;

import jp.aquafactory.apprenticecodex.gametest.spell.targeting.support.TargetingTestFixture;
import jp.aquafactory.apprenticecodex.item.manaforceblade.ManaForceBladeProjectileEntity;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.bloodbrand.BloodBrandKunai;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceDaggerEntity;
import jp.aquafactory.apprenticecodex.spell.mysticshield.MysticShieldProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.skyedge.SkyEdgeProjectileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import java.util.*;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class ProjectileCollisionFixture implements AutoCloseable {
    static final String TEMPLATE = "gametest/projectile_collision";
    static final String BATCH = "apprenticecodex.straight_projectile_collision_isolated";
    final GameTestHelper helper;
    final ServerLevel level;
    final TargetingTestFixture scope;
    final FakePlayer caster;
    final BlockPos wall;

    ProjectileCollisionFixture(GameTestHelper helper) {
        this.helper = helper;
        level = helper.getLevel();
        scope = TargetingTestFixture.of(helper);
        // 初期配置と6tickまでの飛行経路を丸ごと管理領域に収める。相対的な擦過位置は変えない。
        caster = createEquipmentTestPlayer(helper, new BlockPos(8, 6, 4), "projectile_collision_test");
        caster.setNoGravity(true);
        scope.add(caster);
        wall = BlockPos.containing(position(new Vec3(3, 3, 2)));
        BlockState previous = level.getBlockState(wall);
        scope.onClose(() -> level.setBlockAndUpdate(wall, previous));
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
    }

    Vec3 position(Vec3 originalRelative) {
        return helper.absoluteVec(originalRelative.add(8, 4, 4));
    }

    Map<String, Projectile> projectiles(Vec3 position) {
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
            scope.add(projectile);
        });
        return projectiles;
    }

    BlockImpacts observe(Map<String, Projectile> projectiles, boolean cancel) {
        return new BlockImpacts(projectiles, cancel);
    }

    final class BlockImpacts {
        private final Map<Projectile, List<BlockHitResult>> hits = new IdentityHashMap<>();
        private final Consumer<ProjectileImpactEvent> listener;

        BlockImpacts(Map<String, Projectile> projectiles, boolean cancel) {
            projectiles.values().forEach(projectile -> hits.put(projectile, new ArrayList<>()));
            listener = event -> {
                var ownHits = hits.get(event.getProjectile());
                if (ownHits != null && event.getRayTraceResult() instanceof BlockHitResult hit) {
                    ownHits.add(hit);
                    if (cancel) event.setCanceled(true);
                }
            };
            scope.onClose(() -> NeoForge.EVENT_BUS.unregister(listener));
            NeoForge.EVENT_BUS.addListener(listener);
        }

        int count(Projectile projectile) {
            return hits.get(projectile).size();
        }

        void assertHit(String name, Projectile projectile) {
            var ownHits = hits.get(projectile);
            helper.assertTrue(!ownHits.isEmpty() && ownHits.stream().allMatch(hit -> hit.getBlockPos().equals(wall)),
                    name + " must independently impact the fixture wall: " + diagnostics(projectile));
        }

        String diagnostics(Projectile projectile) {
            return "impacts=" + hits.get(projectile).stream().map(hit -> hit.getBlockPos() + "@" + hit.getLocation()).toList()
                    + ", age=" + projectile.tickCount + ", velocity=" + projectile.getDeltaMovement() + ", " + scope.diagnostics(projectile);
        }
    }

    @Override
    public void close() {
        scope.close();
    }
}
