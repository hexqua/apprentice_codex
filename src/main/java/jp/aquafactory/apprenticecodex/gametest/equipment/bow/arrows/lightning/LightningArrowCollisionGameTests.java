package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.lightning;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.ArrowTestPositions;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.BowTestScope;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowCollision;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LightningArrowCollisionGameTests {
    private LightningArrowCollisionGameTests() {
    }

    private static final String TEMPLATE = "gametest/arrow_combat";
    private static final String BATCH = "apprenticecodex.lightning_arrow";

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void piercesSmallTargetsBetweenTicksAndHitsEachOnlyOnce(GameTestHelper helper) {
        try (var scene = new LightningArrowFixture(helper)) {
            var first = scene.zombie(new Vec3(2.6, 2, 3.5));
            first.setBaby(true);
            var second = scene.zombie(new Vec3(4.8, 2, 3.5));
            var third = scene.zombie(new Vec3(7.0, 2, 3.5));
            var arrow = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(1.5, 2.5, 3.5)), new Vec3(1, 0, 0), 8);
            BowTestScope.tickOwned(arrow, false);
            helper.assertTrue(first.getHealth() < 20 && second.getHealth() < 20,
                    "A single movement segment must hit every intersecting target, including a small target");
            var after = second.getHealth();
            // 同じ個体を次の区間へ移し、無敵時間で重複防止が隠れないようにする。
            second.setPos(ArrowTestPositions.absolute(helper, new Vec3(7.5, 2, 3.5)));
            second.invulnerableTime = 0;
            BowTestScope.tickOwned(arrow, false);
            helper.assertTrue(second.getHealth() == after && third.getHealth() < 20,
                    "The same arrow must skip a previous target while still hitting new targets");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void rejectedDamageIsNotRetriedOnLaterSegments(GameTestHelper helper) {
        try (var scene = new LightningArrowFixture(helper)) {
            var target = scene.zombie(new Vec3(4.8, 2, 3.5));
            target.setInvulnerable(true);
            var arrow = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(1.5, 2.5, 3.5)), new Vec3(1, 0, 0), 8);
            BowTestScope.tickOwned(arrow, false);
            target.setInvulnerable(false);
            target.setPos(ArrowTestPositions.absolute(helper, new Vec3(7.5, 2, 3.5)));
            BowTestScope.tickOwned(arrow, false);
            helper.assertTrue(target.getHealth() == 20, "Rejected damage must still consume this arrow's target contact");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void widthIncludesNearbyTargetsButRejectsOutsideAndBeyondRange(GameTestHelper helper) {
        try (var scene = new LightningArrowFixture(helper)) {
            var near = scene.zombie(new Vec3(3.5, 2, 4.7));
            var outside = scene.zombie(new Vec3(3.5, 2, 4.9));
            var beyond = scene.zombie(new Vec3(5.9, 2, 3.5));
            var arrow = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(1.5, 2.5, 3.5)), new Vec3(1, 0, 0), 4);
            BowTestScope.tickOwned(arrow, false);
            helper.assertTrue(near.getHealth() < 20, "An exposed target within one block of the beam must be hit");
            helper.assertTrue(outside.getHealth() == 20, "A target outside the beam width must not be hit");
            helper.assertTrue(beyond.getHealth() == 20, "Expanded bounds must not extend damage beyond the range cap");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void wallStopsArrowAfterFrontTargetWithoutDamagingBackTarget(GameTestHelper helper) {
        try (var scene = new LightningArrowFixture(helper)) {
            var front = scene.zombie(new Vec3(2.5, 2, 3.5));
            var back = scene.zombie(new Vec3(4.5, 2, 3.5));
            ArrowTestPositions.setBlock(helper, new BlockPos(3, 2, 3), Blocks.STONE);
            ArrowTestPositions.setBlock(helper, new BlockPos(3, 3, 3), Blocks.STONE);
            var arrow = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(1.5, 2.5, 3.5)), new Vec3(1, 0, 0), 8);
            BowTestScope.tickOwned(arrow, false);
            helper.assertTrue(front.getHealth() < 20 && back.getHealth() == 20,
                    "Entity penetration must stop at the first blocking terrain");
            helper.assertTrue(arrow.stopped() && Math.abs(arrow.traveledDistance() - 1.5) < 1.0e-5,
                    "Wall collision must clamp flight and trail distance to the block surface");
            var replica = new LightningArrowEntity(EntityRegistry.LIGHTNING_ARROW.get(), helper.getLevel());
            replica.getEntityData().assignValues(arrow.getEntityData().getNonDefaultValues());
            helper.assertTrue(replica.stopped() && replica.traveledDistance() == arrow.traveledDistance()
                            && replica.flight().point(replica.traveledDistance()).distanceTo(arrow.position()) < 1.0e-5,
                    "Tracked metadata must reconstruct the stopped arrow at the wall surface");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void centerlinePassesNarrowGapButSideWallShieldsTarget(GameTestHelper helper) {
        try (var scene = new LightningArrowFixture(helper)) {
            // 敵判定には幅を持たせるが、側壁をすり抜けて敵へ届かせない。
            var hidden = scene.zombie(new Vec3(3.5, 2, 4.95));
            var exposed = scene.zombie(new Vec3(2.5, 2, 4.55));
            for (int x = 1; x <= 6; x++) {
                ArrowTestPositions.setBlock(helper, new BlockPos(x, 2, 4), Blocks.IRON_BARS);
                ArrowTestPositions.setBlock(helper, new BlockPos(x, 3, 4), Blocks.IRON_BARS);
            }
            // 接続した鉄格子の面はz=4.4375..4.5625。隠れた個体のAABB全体をその奥へ置く。
            var arrow = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(1.5, 2.5, 4.2)), new Vec3(1, 0, 0), 4);
            BowTestScope.tickOwned(arrow, false);
            helper.assertTrue(Math.abs(arrow.traveledDistance() - 4) < 1.0e-5,
                    "The centerline must pass even when the widened attack overlaps side terrain");
            helper.assertTrue(hidden.getHealth() == 20, "A side wall must occlude targets inside the attack width");
            helper.assertTrue(exposed.getHealth() < 20, "A body protruding through the cover must remain hittable");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void blockCollisionUsesSlabShapeAndIgnoresWater(GameTestHelper helper) {
        try (var scene = new LightningArrowFixture(helper)) {
            ArrowTestPositions.setBlock(helper, new BlockPos(3, 3, 3), Blocks.STONE_SLAB);
            var low = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(1.5, 3.25, 3.5)), new Vec3(1, 0, 0), 5);
            low.tick();
            helper.assertTrue(Math.abs(low.traveledDistance() - 1.5) < 1.0e-5, "A bottom slab must stop the lower shot");
            var high = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(1.5, 3.75, 3.5)), new Vec3(1, 0, 0), 5);
            high.tick();
            helper.assertTrue(high.traveledDistance() == 5, "The shot must pass through the empty half of a slab block");
            ArrowTestPositions.setBlock(helper, new BlockPos(3, 3, 3), Blocks.WATER);
            var water = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(1.5, 3.25, 3.5)), new Vec3(1, 0, 0), 5);
            water.tick();
            helper.assertTrue(water.traveledDistance() == 5, "Water must neither stop nor slow the shot");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void protectsCasterAndOwnedSummonsWhilePiercingToEnemy(GameTestHelper helper) {
        try (var scene = new LightningArrowFixture(helper)) {
            var wolf = EntityType.WOLF.create(helper.getLevel());
            wolf.setTame(true, false);
            wolf.setOwnerUUID(scene.owner.getUUID());
            wolf.setNoAi(true);
            wolf.setPos(ArrowTestPositions.absolute(helper, new Vec3(3, 2, 3.5)));
            scene.add(wolf);
            var enemy = scene.zombie(new Vec3(5, 2, 3.5));
            scene.owner.setPos(ArrowTestPositions.absolute(helper, new Vec3(2, 2, 3.5)));
            var ownerHealth = scene.owner.getHealth();
            var wolfHealth = wolf.getHealth();
            var arrow = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(1.5, 2.5, 3.5)), new Vec3(1, 0, 0), 5);
            BowTestScope.tickOwned(arrow, false);
            helper.assertTrue(scene.owner.getHealth() == ownerHealth && wolf.getHealth() == wolfHealth,
                    "The caster and owned creatures must be protected");
            helper.assertTrue(enemy.getHealth() < 20, "Protected targets must not stop penetration");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void multipartContactsResolveToOneParent(GameTestHelper helper) {
        try (var scene = new LightningArrowFixture(helper)) {
            var dragon = EntityType.ENDER_DRAGON.create(helper.getLevel());
            dragon.setNoAi(true);
            var far = ArrowTestPositions.absolute(helper, new Vec3(20, 15, 20));
            dragon.setPos(far);
            scene.add(dragon);
            for (var part : dragon.getSubEntities()) part.setPos(far);
            dragon.getSubEntities()[0].setPos(ArrowTestPositions.absolute(helper, new Vec3(3, 2, 3.5)));
            dragon.getSubEntities()[1].setPos(ArrowTestPositions.absolute(helper, new Vec3(5, 2, 3.5)));
            var start = ArrowTestPositions.absolute(helper, new Vec3(1.5, 2.5, 3.5));
            var arrow = scene.arrow(start, new Vec3(1, 0, 0), 5);
            var hits = LightningArrowCollision.contacts(helper.getLevel(), arrow, scene.owner, start, start.add(5, 0, 0));
            helper.assertTrue(hits.size() == 1 && hits.getFirst().target() == dragon,
                    "Distinct intersecting parts must resolve to one contact on the parent");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void nearWallAndStartingInsideTerrainCannotSkipToTargets(GameTestHelper helper) {
        try (var scene = new LightningArrowFixture(helper)) {
            ArrowTestPositions.setBlock(helper, new BlockPos(2, 2, 3), Blocks.STONE);
            var target = scene.zombie(new Vec3(3.5, 2, 3.5));
            var near = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(1.99, 2.5, 3.5)), new Vec3(1, 0, 0), 5);
            near.tick();
            helper.assertTrue(near.stopped() && near.traveledDistance() < 0.02,
                    "A wall immediately in front of the origin must stop the shot");
            var inside = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(2.5, 2.5, 3.5)), new Vec3(1, 0, 0), 5);
            inside.tick();
            helper.assertTrue(inside.stopped() && inside.traveledDistance() < 1.0e-5 && target.getHealth() == 20,
                    "Starting inside a collider must not let the shot emerge through terrain");
        }
        helper.succeed();
    }
}
