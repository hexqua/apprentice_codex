package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.packet.LightningArrowImpactPacket;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowCollision;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowEntity;
import jp.aquafactory.apprenticecodex.spell.lightningarrow.LightningArrowFlight;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LightningArrowGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.lightning_arrow";

    private LightningArrowGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void flightMaintainsFiveBlocksInEveryDirectionAndStopsAtRange(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var origin = helper.absoluteVec(new Vec3(3.5, 80, 3.5));
            for (var direction : List.of(new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                    new Vec3(0, 1, 0), new Vec3(0, -1, 0), new Vec3(1, 1, 1).normalize())) {
                var arrow = scene.arrow(origin, direction, 64);
                for (int tick = 1; tick <= 13; tick++) {
                    arrow.tick();
                    var expected = Math.min(64, tick * 5);
                    helper.assertTrue(Math.abs(arrow.traveledDistance() - expected) < 1.0e-4,
                            "Arrow must travel five blocks per tick and trim its final step");
                    helper.assertTrue(arrow.position().distanceTo(origin.add(direction.scale(expected))) < 1.0e-4,
                            "Arrow must remain straight without gravity or direction-dependent speed");
                }
                helper.assertTrue(arrow.stopped(), "Arrow must stop at its range");
                var end = arrow.position();
                for (int tick = 0; tick < LightningArrowEntity.TRAIL_TICKS; tick++) arrow.tick();
                helper.assertTrue(arrow.isRemoved() && arrow.position().equals(end),
                        "Stopped arrow must remain stationary and expire after its trail");
                helper.assertFalse(arrow.shouldBeSaved(), "Lightning arrows must not persist in world saves");
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void piercesSmallTargetsBetweenTicksAndHitsEachOnlyOnce(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var first = scene.zombie(new Vec3(2.6, 2, 3.5));
            first.setBaby(true);
            var second = scene.zombie(new Vec3(4.8, 2, 3.5));
            var third = scene.zombie(new Vec3(7.0, 2, 3.5));
            var arrow = scene.arrow(helper.absoluteVec(new Vec3(1.5, 2.5, 3.5)), new Vec3(1, 0, 0), 8);
            arrow.tick();
            helper.assertTrue(first.getHealth() < 20 && second.getHealth() < 20,
                    "A single movement segment must hit every intersecting target, including a small target");
            var after = second.getHealth();
            // 同じ個体を次の区間へ移し、無敵時間で重複防止が隠れないようにする。
            second.setPos(helper.absoluteVec(new Vec3(7.5, 2, 3.5)));
            second.invulnerableTime = 0;
            arrow.tick();
            helper.assertTrue(second.getHealth() == after && third.getHealth() < 20,
                    "The same arrow must skip a previous target while still hitting new targets");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void rejectedDamageIsNotRetriedOnLaterSegments(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var target = scene.zombie(new Vec3(4.8, 2, 3.5));
            target.setInvulnerable(true);
            var arrow = scene.arrow(helper.absoluteVec(new Vec3(1.5, 2.5, 3.5)), new Vec3(1, 0, 0), 8);
            arrow.tick();
            target.setInvulnerable(false);
            target.setPos(helper.absoluteVec(new Vec3(7.5, 2, 3.5)));
            arrow.tick();
            helper.assertTrue(target.getHealth() == 20, "Rejected damage must still consume this arrow's target contact");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void widthIncludesNearbyTargetsButRejectsOutsideAndBeyondRange(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var near = scene.zombie(new Vec3(3.5, 2, 4.7));
            var outside = scene.zombie(new Vec3(3.5, 2, 4.9));
            var beyond = scene.zombie(new Vec3(5.9, 2, 3.5));
            var arrow = scene.arrow(helper.absoluteVec(new Vec3(1.5, 2.5, 3.5)), new Vec3(1, 0, 0), 4);
            arrow.tick();
            helper.assertTrue(near.getHealth() < 20, "An exposed target within one block of the beam must be hit");
            helper.assertTrue(outside.getHealth() == 20, "A target outside the beam width must not be hit");
            helper.assertTrue(beyond.getHealth() == 20, "Expanded bounds must not extend damage beyond the range cap");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void wallStopsArrowAfterFrontTargetWithoutDamagingBackTarget(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var front = scene.zombie(new Vec3(2.5, 2, 3.5));
            var back = scene.zombie(new Vec3(4.5, 2, 3.5));
            helper.setBlock(new BlockPos(3, 2, 3), Blocks.STONE);
            helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE);
            var arrow = scene.arrow(helper.absoluteVec(new Vec3(1.5, 2.5, 3.5)), new Vec3(1, 0, 0), 8);
            arrow.tick();
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
        try (var scene = new Scene(helper)) {
            // 敵判定には幅を持たせるが、側壁をすり抜けて敵へ届かせない。
            var hidden = scene.zombie(new Vec3(3.5, 2, 4.95));
            var exposed = scene.zombie(new Vec3(2.5, 2, 4.55));
            for (int x = 1; x <= 6; x++) {
                helper.setBlock(new BlockPos(x, 2, 4), Blocks.IRON_BARS);
                helper.setBlock(new BlockPos(x, 3, 4), Blocks.IRON_BARS);
            }
            // 接続した鉄格子の面はz=4.4375..4.5625。隠れた個体のAABB全体をその奥へ置く。
            var arrow = scene.arrow(helper.absoluteVec(new Vec3(1.5, 2.5, 4.2)), new Vec3(1, 0, 0), 4);
            arrow.tick();
            helper.assertTrue(Math.abs(arrow.traveledDistance() - 4) < 1.0e-5,
                    "The centerline must pass even when the widened attack overlaps side terrain");
            helper.assertTrue(hidden.getHealth() == 20, "A side wall must occlude targets inside the attack width");
            helper.assertTrue(exposed.getHealth() < 20, "A body protruding through the cover must remain hittable");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void blockCollisionUsesSlabShapeAndIgnoresWater(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE_SLAB);
            var low = scene.arrow(helper.absoluteVec(new Vec3(1.5, 3.25, 3.5)), new Vec3(1, 0, 0), 5);
            low.tick();
            helper.assertTrue(Math.abs(low.traveledDistance() - 1.5) < 1.0e-5, "A bottom slab must stop the lower shot");
            var high = scene.arrow(helper.absoluteVec(new Vec3(1.5, 3.75, 3.5)), new Vec3(1, 0, 0), 5);
            high.tick();
            helper.assertTrue(high.traveledDistance() == 5, "The shot must pass through the empty half of a slab block");
            helper.setBlock(new BlockPos(3, 3, 3), Blocks.WATER);
            var water = scene.arrow(helper.absoluteVec(new Vec3(1.5, 3.25, 3.5)), new Vec3(1, 0, 0), 5);
            water.tick();
            helper.assertTrue(water.traveledDistance() == 5, "Water must neither stop nor slow the shot");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void protectsCasterAndOwnedSummonsWhilePiercingToEnemy(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var wolf = EntityType.WOLF.create(helper.getLevel());
            wolf.setTame(true, false);
            wolf.setOwnerUUID(scene.owner.getUUID());
            wolf.setNoAi(true);
            wolf.setPos(helper.absoluteVec(new Vec3(3, 2, 3.5)));
            scene.add(wolf);
            var enemy = scene.zombie(new Vec3(5, 2, 3.5));
            scene.owner.setPos(helper.absoluteVec(new Vec3(2, 2, 3.5)));
            var ownerHealth = scene.owner.getHealth();
            var wolfHealth = wolf.getHealth();
            var arrow = scene.arrow(helper.absoluteVec(new Vec3(1.5, 2.5, 3.5)), new Vec3(1, 0, 0), 5);
            arrow.tick();
            helper.assertTrue(scene.owner.getHealth() == ownerHealth && wolf.getHealth() == wolfHealth,
                    "The caster and owned creatures must be protected");
            helper.assertTrue(enemy.getHealth() < 20, "Protected targets must not stop penetration");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void multipartContactsResolveToOneParent(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var dragon = EntityType.ENDER_DRAGON.create(helper.getLevel());
            dragon.setNoAi(true);
            var far = helper.absoluteVec(new Vec3(20, 15, 20));
            dragon.setPos(far);
            scene.add(dragon);
            for (var part : dragon.getSubEntities()) part.setPos(far);
            dragon.getSubEntities()[0].setPos(helper.absoluteVec(new Vec3(3, 2, 3.5)));
            dragon.getSubEntities()[1].setPos(helper.absoluteVec(new Vec3(5, 2, 3.5)));
            var start = helper.absoluteVec(new Vec3(1.5, 2.5, 3.5));
            var arrow = scene.arrow(start, new Vec3(1, 0, 0), 5);
            var hits = LightningArrowCollision.contacts(helper.getLevel(), arrow, scene.owner, start, start.add(5, 0, 0));
            helper.assertTrue(hits.size() == 1 && hits.getFirst().target() == dragon,
                    "Distinct intersecting parts must resolve to one contact on the parent");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void flightAndImpactDataRoundTripWithoutVelocityClamping(GameTestHelper helper) {
        for (var direction : List.of(new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(1, -1, 1))) {
            var flight = new LightningArrowFlight(new Vec3(12345678.125, 65.625, -12345678.375), direction, 5, 64, 123);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeNbt(flight.encode());
                var restored = LightningArrowFlight.decode(buffer.readNbt());
                helper.assertTrue(restored.origin().equals(flight.origin()) && restored.seed() == flight.seed(),
                        "Flight synchronization must preserve position precision and trail seed");
                helper.assertTrue(Math.abs(restored.point(5).distanceTo(restored.origin()) - 5) < 1.0e-7,
                        "Five-block motion must survive synchronization in every direction");
                helper.assertTrue(restored.point(100).distanceTo(flight.point(64)) < 1.0e-7,
                        "Client flight reconstruction must clamp to the range");
            } finally {
                buffer.release();
            }
        }
        var packet = new LightningArrowImpactPacket(new Vec3(1, 2, 3), new Vec3(0, 0, 1), true);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            LightningArrowImpactPacket.STREAM_CODEC.encode(buffer, packet);
            helper.assertTrue(packet.equals(LightningArrowImpactPacket.STREAM_CODEC.decode(buffer)),
                    "Impact synchronization must preserve its position, direction and terrain flag");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void castingUsesArrowOffsetAndServerTicksAdvanceTheArrow(GameTestHelper helper) {
        var scene = new Scene(helper);
        scene.owner.setPos(helper.absoluteVec(new Vec3(2.5, 20, 2.5)));
        scene.owner.setYRot(0);
        scene.owner.setXRot(0);
        var eye = scene.owner.getEyePosition();
        SpellRegistry.LIGHTNING_ARROW.get().onCast(helper.getLevel(), 1, scene.owner,
                CastSource.SPELLBOOK, MagicData.getPlayerMagicData(scene.owner));
        var arrows = helper.getLevel().getEntitiesOfClass(LightningArrowEntity.class, scene.owner.getBoundingBox().inflate(2));
        helper.assertTrue(arrows.size() == 1, "A completed cast must spawn exactly one arrow");
        var arrow = arrows.getFirst();
        scene.entities.add(arrow);
        helper.assertTrue(arrow.position().distanceTo(eye.add(0, -0.4, 1)) < 1.0e-7,
                "The cast must start one block forward and 0.4 blocks below the eye");
        helper.runAfterDelay(2, () -> {
            try (scene) {
                helper.assertTrue(arrow.traveledDistance() >= 5 && arrow.traveledDistance() <= 15,
                        "The registered projectile must advance during normal server ticks");
                helper.assertTrue(arrow.flight().range() == 64, "Casting must use the spell range");
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void nearWallAndStartingInsideTerrainCannotSkipToTargets(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            helper.setBlock(new BlockPos(2, 2, 3), Blocks.STONE);
            var target = scene.zombie(new Vec3(3.5, 2, 3.5));
            var near = scene.arrow(helper.absoluteVec(new Vec3(1.99, 2.5, 3.5)), new Vec3(1, 0, 0), 5);
            near.tick();
            helper.assertTrue(near.stopped() && near.traveledDistance() < 0.02,
                    "A wall immediately in front of the origin must stop the shot");
            var inside = scene.arrow(helper.absoluteVec(new Vec3(2.5, 2.5, 3.5)), new Vec3(1, 0, 0), 5);
            inside.tick();
            helper.assertTrue(inside.stopped() && inside.traveledDistance() < 1.0e-5 && target.getHealth() == 20,
                    "Starting inside a collider must not let the shot emerge through terrain");
        }
        helper.succeed();
    }

    private static final class Scene implements AutoCloseable {
        private final GameTestHelper helper;
        private final List<Entity> entities = new ArrayList<>();
        private final Map<BlockPos, net.minecraft.world.level.block.state.BlockState> barriers = new LinkedHashMap<>();
        private final FakePlayer owner;

        private Scene(GameTestHelper helper) {
            this.helper = helper;
            // basic_floorは5x3x5で外周に自動バリアが置かれる。高速弾用の区間を確保し、終了時に戻す。
            for (var local : BlockPos.betweenClosed(new BlockPos(-1, 0, -1), new BlockPos(5, 3, 5))) {
                var position = helper.absolutePos(local);
                var state = helper.getLevel().getBlockState(position);
                if (state.is(Blocks.BARRIER)) {
                    barriers.put(position.immutable(), state);
                    helper.getLevel().setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
                }
            }
            owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "lightning_test"));
            owner.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
            add(owner);
        }

        private void add(Entity entity) {
            entities.add(entity);
            helper.getLevel().addFreshEntity(entity);
        }

        private Zombie zombie(Vec3 localPosition) {
            var entity = EntityType.ZOMBIE.create(helper.getLevel());
            entity.setNoAi(true);
            entity.setNoGravity(true);
            entity.getAttribute(Attributes.ARMOR).setBaseValue(0);
            entity.setPos(helper.absoluteVec(localPosition));
            add(entity);
            return entity;
        }

        private LightningArrowEntity arrow(Vec3 origin, Vec3 direction, double range) {
            var arrow = new LightningArrowEntity(EntityRegistry.LIGHTNING_ARROW.get(), helper.getLevel());
            arrow.launch(owner, origin, direction, range, 2);
            add(arrow);
            return arrow;
        }

        @Override
        public void close() {
            entities.forEach(Entity::discard);
            barriers.forEach((position, state) -> helper.getLevel().setBlockAndUpdate(position, state));
        }
    }
}
