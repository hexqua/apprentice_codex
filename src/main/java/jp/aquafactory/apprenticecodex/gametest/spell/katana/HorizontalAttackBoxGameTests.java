package jp.aquafactory.apprenticecodex.gametest.spell.katana;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.utility.RaycastTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.katana.KatanaHitFixture.BATCH;
import static jp.aquafactory.apprenticecodex.gametest.spell.katana.KatanaHitFixture.TEMPLATE;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class HorizontalAttackBoxGameTests {
    private HorizontalAttackBoxGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void horizontalOrientedBoxRejectsBroadPhaseCorners(GameTestHelper helper) {
        try (var scene = new KatanaHitFixture(helper)) {
            var level = scene.level;
            var source = scene.player("katana_oriented_box_source", new Vec3(1.5D, 2.0D, 1.5D));
            var faceCenter = scene.position(new Vec3(4.5D, 3.0D, 4.5D));
            var forward = new Vec3(1.0D, 0.0D, 1.0D).normalize();
            var attackBox = new RaycastTools.HorizontalOrientedBox(faceCenter, forward, 1.0D, 0.75D, 2.0D);

            var inside = scene.zombie(faceCenter.add(forward).add(0.0D, -0.5D, 0.0D));
            var broadPhaseCorner = scene.zombie(scene.position(new Vec3(6.8D, 2.5D, 4.5D)));
            var behind = scene.zombie(faceCenter.subtract(forward.scale(0.7D)).add(0.0D, -0.5D, 0.0D));
            var above = scene.zombie(faceCenter.add(forward).add(0.0D, 1.0D, 0.0D));

            var hits = RaycastTools.hitsHorizontalOrientedBox(level, source, attackBox, entity -> true);
            helper.assertTrue(hits.size() == 1 && hits.get(0).entity() == inside,
                    "Horizontal oriented box should reject broad-phase corner, rear, and vertical misses: " + scene.describe(hits));

        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void orientedBoxOcclusionUsesPartialVisibilityAndEmbeddedSource(GameTestHelper helper) {
        try (var scene = new KatanaHitFixture(helper)) {
            var level = scene.level;
            var source = scene.player("katana_occlusion_source", new Vec3(1.5D, 2.0D, 1.5D));
            var faceCenter = scene.position(new Vec3(2.5D, 2.5D, 2.5D));
            var attackBox = new RaycastTools.HorizontalOrientedBox(
                    faceCenter,
                    new Vec3(0.0D, 0.0D, 1.0D),
                    1.0D,
                    0.75D,
                    2.5D
            );
            var target = scene.zombie(scene.position(new Vec3(2.5D, 3.0D, 4.5D)));

            var visibleHit = scene.singleHit(source, attackBox);
            helper.assertFalse(visibleHit.blockOccluded(), "Target without a wall should not be occluded");

            scene.block(new BlockPos(2, 2, 3), Blocks.STONE);
            scene.block(new BlockPos(2, 3, 3), Blocks.STONE);
            scene.block(new BlockPos(2, 4, 3), Blocks.STONE);
            var fullyBlockedHit = scene.singleHit(source, attackBox);
            helper.assertTrue(fullyBlockedHit.blockOccluded(), "Three-block wall should fully occlude the target");

            scene.block(new BlockPos(2, 2, 3), Blocks.STONE_SLAB);
            scene.block(new BlockPos(2, 3, 3), Blocks.AIR);
            scene.block(new BlockPos(2, 4, 3), Blocks.AIR);
            var partiallyVisibleHit = scene.singleHit(source, attackBox);
            helper.assertFalse(partiallyVisibleHit.blockOccluded(),
                    "Target with any visible sampled point should not be occluded");

            scene.block(new BlockPos(2, 2, 2), Blocks.STONE);
            var embeddedSourceHit = scene.singleHit(source, attackBox);
            helper.assertTrue(embeddedSourceHit.blockOccluded(),
                    "All targets should be occluded when the attack origin is inside collision");

        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void orientedBoxDeduplicatesMultipartTargets(GameTestHelper helper) {
        try (var scene = new KatanaHitFixture(helper)) {
            var level = scene.level;
            var source = scene.player("katana_multipart_source", new Vec3(1.5D, 2.0D, 1.5D));
            var faceCenter = scene.position(new Vec3(3.5D, 3.0D, 2.5D));
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
            var farPosition = scene.position(new Vec3(20.0D, 8.0D, 20.0D));
            dragon.setPos(farPosition.x, farPosition.y, farPosition.z);
            scene.add(dragon);

            for (var part : dragon.getSubEntities()) {
                scene.movePart(part, farPosition);
            }
            var firstPartPosition = scene.position(new Vec3(2.5D, 2.5D, 5.0D));
            var secondPartPosition = scene.position(new Vec3(4.5D, 2.5D, 5.5D));
            scene.movePart(dragon.getSubEntities()[0], firstPartPosition);
            scene.movePart(dragon.getSubEntities()[1], secondPartPosition);

            var hits = RaycastTools.hitsHorizontalOrientedBox(level, source, attackBox, entity -> true);
            helper.assertTrue(hits.size() == 1 && hits.get(0).entity() == dragon,
                    "Multiple intersecting parts should resolve to one parent hit: " + scene.describe(hits));

            for (var x = 1; x <= 3; ++x) {
                for (var y = 1; y <= 4; ++y) {
                    scene.block(new BlockPos(x, y, 4), Blocks.STONE);
                }
            }
            var partiallyVisibleHits = RaycastTools.hitsHorizontalOrientedBox(
                    level,
                    source,
                    attackBox,
                    entity -> true
            );
            helper.assertTrue(partiallyVisibleHits.size() == 1 && !partiallyVisibleHits.get(0).blockOccluded(),
                    "A visible intersecting part should make the deduplicated parent hit visible: " + scene.describe(partiallyVisibleHits));

        }
        helper.succeed();
    }
}
