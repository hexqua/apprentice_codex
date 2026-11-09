package jp.aquafactory.apprenticecodex.gametest.spell.katana;

import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static jp.aquafactory.apprenticecodex.gametest.spell.higanbana.HiganbanaGameTestScenarios.discard;
import static jp.aquafactory.apprenticecodex.gametest.spell.higanbana.KatanaAreaHitGameTestScenarios.createPlayer;
import static jp.aquafactory.apprenticecodex.gametest.spell.higanbana.KatanaAreaHitGameTestScenarios.createZombie;
import static jp.aquafactory.apprenticecodex.gametest.spell.katana.KatanaHitFixture.singleHit;

final class HorizontalAttackBoxScenarios {
    private HorizontalAttackBoxScenarios() {}

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
}
