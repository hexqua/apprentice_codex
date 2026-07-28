package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

final class KatanaAreaHitGameTestScenarios {
    private static final double POSITION_EPSILON = 1.0E-6D;
    private static final float HEALTH_EPSILON = 1.0E-4F;

    private KatanaAreaHitGameTestScenarios() {
    }

    static void horizontalOrientedBoxRejectsBroadPhaseCorners(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = createPlayer(helper, "katana_oriented_box_source", new Vec3(1.5D, 2.0D, 1.5D));
        var faceCenter = helper.absoluteVec(new Vec3(4.5D, 3.0D, 4.5D));
        var forward = new Vec3(1.0D, 0.0D, 1.0D).normalize();
        var attackBox = new RaycastTools.HorizontalOrientedBox(faceCenter, forward, 1.0D, 0.75D, 2.0D);

        var inside = createZombie(level, faceCenter.add(forward).add(0.0D, -0.5D, 0.0D));
        var broadPhaseCorner = createZombie(level, helper.absoluteVec(new Vec3(6.8D, 2.5D, 4.5D)));
        var behind = createZombie(level, faceCenter.subtract(forward.scale(0.7D)).add(0.0D, -0.5D, 0.0D));
        var above = createZombie(level, faceCenter.add(forward).add(0.0D, 1.0D, 0.0D));

        var hits = RaycastTools.hitsHorizontalOrientedBox(level, source, attackBox, entity -> true);
        helper.assertTrue(hits.size() == 1 && hits.get(0).entity() == inside,
                "Horizontal oriented box should reject broad-phase corner, rear, and vertical misses");

        discard(source, inside, broadPhaseCorner, behind, above);
        helper.succeed();
    }

    static void orientedBoxOcclusionUsesPartialVisibilityAndEmbeddedSource(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = createPlayer(helper, "katana_occlusion_source", new Vec3(1.5D, 2.0D, 1.5D));
        var faceCenter = helper.absoluteVec(new Vec3(2.5D, 2.5D, 2.5D));
        var attackBox = new RaycastTools.HorizontalOrientedBox(
                faceCenter,
                new Vec3(0.0D, 0.0D, 1.0D),
                1.0D,
                0.75D,
                4.5D
        );
        var target = createZombie(level, helper.absoluteVec(new Vec3(2.5D, 2.0D, 6.5D)));

        var visibleHit = singleHit(helper, level, source, attackBox);
        helper.assertFalse(visibleHit.blockOccluded(), "Target without a wall should not be occluded");

        helper.setBlock(new BlockPos(2, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 3, 4), Blocks.STONE);
        var fullyBlockedHit = singleHit(helper, level, source, attackBox);
        helper.assertTrue(fullyBlockedHit.blockOccluded(), "Two-block wall should fully occlude the target");

        helper.setBlock(new BlockPos(2, 3, 4), Blocks.AIR);
        var partiallyVisibleHit = singleHit(helper, level, source, attackBox);
        helper.assertFalse(partiallyVisibleHit.blockOccluded(),
                "Target with any visible sampled point should not be occluded");

        helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        var embeddedSourceHit = singleHit(helper, level, source, attackBox);
        helper.assertTrue(embeddedSourceHit.blockOccluded(),
                "All targets should be occluded when the attack origin is inside collision");

        discard(source, target);
        helper.succeed();
    }

    static void orientedBoxDeduplicatesMultipartTargets(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = createPlayer(helper, "katana_multipart_source", new Vec3(1.5D, 2.0D, 1.5D));
        var faceCenter = helper.absoluteVec(new Vec3(3.5D, 3.0D, 2.5D));
        var attackBox = new RaycastTools.HorizontalOrientedBox(
                faceCenter,
                new Vec3(0.0D, 0.0D, 1.0D),
                3.0D,
                1.5D,
                5.0D
        );

        var dragon = EntityType.ENDER_DRAGON.create(level);
        if (dragon == null) {
            throw new IllegalStateException("Failed to create multipart test dragon");
        }
        dragon.setNoAi(true);
        var farPosition = helper.absoluteVec(new Vec3(20.0D, 8.0D, 20.0D));
        dragon.setPos(farPosition.x, farPosition.y, farPosition.z);
        level.addFreshEntity(dragon);

        for (var part : dragon.getSubEntities()) {
            part.setPos(farPosition.x, farPosition.y, farPosition.z);
        }
        var firstPartPosition = helper.absoluteVec(new Vec3(2.5D, 2.5D, 5.0D));
        var secondPartPosition = helper.absoluteVec(new Vec3(4.5D, 2.5D, 5.5D));
        dragon.getSubEntities()[0].setPos(firstPartPosition.x, firstPartPosition.y, firstPartPosition.z);
        dragon.getSubEntities()[1].setPos(secondPartPosition.x, secondPartPosition.y, secondPartPosition.z);

        var hits = RaycastTools.hitsHorizontalOrientedBox(level, source, attackBox, entity -> true);
        helper.assertTrue(hits.size() == 1 && hits.get(0).entity() == dragon,
                "Multiple intersecting parts should resolve to one parent hit");

        for (var x = 1; x <= 3; ++x) {
            for (var y = 1; y <= 4; ++y) {
                helper.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
            }
        }
        var partiallyVisibleHits = RaycastTools.hitsHorizontalOrientedBox(
                level,
                source,
                attackBox,
                entity -> true
        );
        helper.assertTrue(partiallyVisibleHits.size() == 1 && !partiallyVisibleHits.get(0).blockOccluded(),
                "A visible intersecting part should make the deduplicated parent hit visible");

        discard(source, dragon);
        helper.succeed();
    }

    static void slashBladeRefreshesPoseAndDamagesThroughWall(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "slash_blade_area_owner", new Vec3(2.5D, 2.0D, 2.5D));
        owner.setYRot(0.0F);
        var weapon = new SlashBladeKatanaEntity(EntityRegistry.SLASH_BLADE_KATANA.get(), level, owner);
        weapon.setDamage(4.0F);
        level.addFreshEntity(weapon);

        var movedPosition = helper.absoluteVec(new Vec3(5.5D, 2.0D, 3.5D));
        owner.setPos(movedPosition.x, movedPosition.y, movedPosition.z);
        owner.setYRot(-90.0F);
        var expectedWeaponPosition = RotationTools.calculateBehindPosition(owner, -0.5D, 0.0D, -0.75D);
        var target = createZombie(level, expectedWeaponPosition.add(2.0D, 0.0D, 0.0D));
        var initialHealth = target.getHealth();
        helper.setBlock(new BlockPos(7, 2, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(7, 3, 3), Blocks.STONE);

        weapon.slash(level);

        helper.assertTrue(weapon.position().distanceTo(expectedWeaponPosition) < POSITION_EPSILON,
                "Slash Blade should refresh its position when the slash executes");
        helper.assertTrue(Math.abs(weapon.getYRot() - owner.getYRot()) < POSITION_EPSILON,
                "Slash Blade should refresh its horizontal direction when the slash executes");
        helper.assertTrue(target.getHealth() < initialHealth - HEALTH_EPSILON,
                "Slash Blade should damage a target through a wall");

        discard(owner, weapon, target);
        helper.succeed();
    }

    static void higanbanaStaysHorizontalAndRejectsWallHits(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "higanbana_area_owner", new Vec3(2.5D, 2.0D, 2.5D));
        owner.setYRot(0.0F);
        owner.setXRot(60.0F);
        var weapon = new HiganbanaKatanaEntity(EntityRegistry.HIGANBANA_KATANA.get(), level, owner);
        weapon.setDamage(4.0F);
        weapon.setRemainingSlashCount(2);
        level.addFreshEntity(weapon);

        var target = createZombie(level, weapon.position().add(0.0D, 0.0D, 2.0D));
        var initialHealth = target.getHealth();
        helper.setBlock(new BlockPos(2, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 3, 4), Blocks.STONE);

        weapon.slash(level);
        helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < HEALTH_EPSILON,
                "Higanbana should not damage a fully occluded target");
        helper.assertTrue(Math.abs(weapon.getXRot()) < POSITION_EPSILON,
                "Higanbana should remain horizontal regardless of owner pitch");

        helper.setBlock(new BlockPos(2, 2, 4), Blocks.AIR);
        helper.setBlock(new BlockPos(2, 3, 4), Blocks.AIR);
        tickWeapon(level, weapon, 5);
        helper.assertTrue(target.getHealth() < initialHealth - HEALTH_EPSILON,
                "Higanbana should reevaluate the wall and damage on a later slash");

        discard(owner, weapon, target);
        helper.succeed();
    }

    private static RaycastTools.OrientedBoxHit singleHit(
            GameTestHelper helper,
            ServerLevel level,
            Entity source,
            RaycastTools.HorizontalOrientedBox attackBox
    ) {
        var hits = RaycastTools.hitsHorizontalOrientedBox(level, source, attackBox, entity -> true);
        helper.assertTrue(hits.size() == 1, "Expected exactly one oriented-box hit");
        return hits.get(0);
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name, Vec3 localPosition) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        var position = helper.absoluteVec(localPosition);
        player.setPos(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static Zombie createZombie(ServerLevel level, Vec3 position) {
        var zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            throw new IllegalStateException("Failed to create katana area test zombie");
        }
        zombie.setPos(position.x, position.y, position.z);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        return zombie;
    }

    private static void tickWeapon(ServerLevel level, HiganbanaKatanaEntity weapon, int ticks) {
        for (var tick = 0; tick < ticks && !weapon.isRemoved(); ++tick) {
            weapon.tickOnServer(level);
        }
    }

    private static void discard(Entity... entities) {
        for (var entity : entities) {
            entity.discard();
        }
    }
}
