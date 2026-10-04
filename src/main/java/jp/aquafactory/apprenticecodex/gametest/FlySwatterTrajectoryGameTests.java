package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.network.packet.FlySwatterTrailPacket;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterLauncherEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.flyswatter.FlySwatterTrajectory;
import jp.aquafactory.apprenticecodex.spell.lockonray.LockOnRayCurve;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
// GameTestの登録はアノテーション走査によるため、通常のJava参照はない。
@SuppressWarnings("unused")
public final class FlySwatterTrajectoryGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.fly_swatter_trajectory";

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void stationaryFlightMatchesSelectedCurve(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.target(s.origin.add(12, 0, 0));
            var missile = s.missile();
            var plan = FlySwatterTrajectory.select(h.getLevel(), missile, s.origin, new Vec3(1, 0, 0),
                    target.getBoundingBox().getCenter(), 0);
            missile.launch(s.origin, s.origin, new Vec3(1, 0, 0), target, 0);
            h.assertTrue(missile.getArrivalTicks() == plan.arrivalTicks(), "Flight deadline must match the selected plan");
            h.assertTrue(plan.arrivalTicks() >= 4 && plan.arrivalTicks() < 20, "Short flight must use Magic Spear scale timing");
            h.assertTrue(missile.position().distanceTo(plan.curve().position(0.25 / plan.arrivalTicks())) < 1.0e-6,
                    "Missile must spawn a quarter tick along the selected curve");
            h.assertTrue(missile.getDeltaMovement().distanceTo(plan.curve().tangent(0.25 / plan.arrivalTicks())
                    .scale(1.0 / plan.arrivalTicks())) < 1.0e-6,
                    "Spawn velocity must follow the tangent at the advanced position");
            for (int tick = 1; tick <= plan.arrivalTicks() / 2; tick++) {
                s.tick(missile);
                h.assertFalse(missile.isRemoved(), "Clear flight must remain active before arrival");
                h.assertTrue(missile.position().distanceTo(plan.curve().position((tick + 0.25) / plan.arrivalTicks())) < 1.0e-6,
                        "Stationary flight must follow the prechecked curve on tick " + tick);
            }
            h.assertTrue(missile.position().distanceTo(s.origin) > 1, "Missile must move immediately without a boost wait");
            h.assertFalse(missile.shouldBeSaved(), "Transient missiles must not resume without trajectory state after reload");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void movingTargetUpdatesEndpointWithoutExtendingDeadline(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.target(s.origin.add(12, 0, 0));
            var missile = s.missile();
            missile.launch(s.origin, s.origin, new Vec3(1, 0, 0), target, 0);
            int deadline = missile.getArrivalTicks();
            s.tick(missile);
            target.setPos(target.position().add(0, 2, 0));
            for (int tick = 2; tick <= deadline && !missile.isRemoved() && !missile.isBursting(); tick++) s.tick(missile);
            h.assertTrue(missile.getArrivalTicks() == deadline, "Moving targets must not extend the arrival deadline");
            h.assertTrue(missile.isBursting() && missile.position().y >= s.origin.y + 2,
                    "Endpoint tracking must hit the moved target within the original deadline");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void launchAdvanceCannotSkipWallOrCombatTarget(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var origin = s.origin.add(0.3, 0, 0);
            var target = s.target(origin.add(12, 0, 0));
            var missile = s.missile();
            // 発射口の箱は壁の外側だが、0.25tickの前進区間だけで壁に接触する。
            for (int y = -3; y <= 3; y++) for (int z = -3; z <= 3; z++) {
                s.block(BlockPos.containing(origin.add(1, y, z)), Blocks.STONE.defaultBlockState());
            }
            var plan = FlySwatterTrajectory.select(h.getLevel(), missile, origin, new Vec3(1, 0, 0),
                    target.getBoundingBox().getCenter(), 0);
            var hit = FlySwatterTrajectory.firstObstruction(h.getLevel(), missile, plan.curve().prefix(0.25 / plan.arrivalTicks()));
            h.assertTrue(hit != null, "Fixture must obstruct the quarter tick launch advance");
            missile.launch(origin, origin, new Vec3(1, 0, 0), target, 0);
            h.assertTrue(missile.isBursting() && missile.position().distanceTo(Objects.requireNonNull(hit).position()) < 0.08,
                    "Launch advance must impact the wall before the missile is spawned");
        }
        try (var s = new Scene(h)) {
            var target = s.target(s.origin.add(12, 0, 0));
            var missile = s.missile();
            var plan = FlySwatterTrajectory.select(h.getLevel(), missile, s.origin, new Vec3(1, 0, 0),
                    target.getBoundingBox().getCenter(), 0);
            var obstacle = s.target(s.origin.add(plan.curve().startTangent().normalize().scale(0.9)).add(0, -0.975, 0));
            h.assertFalse(obstacle.getBoundingBox().inflate(0.3).contains(s.origin),
                    "Launch fixture must place the intervening target beyond the muzzle");
            float health = obstacle.getHealth();
            missile.launch(s.origin, s.origin, new Vec3(1, 0, 0), target, 0);
            h.assertTrue(missile.isBursting() && obstacle.getHealth() < health,
                    "Launch advance must damage an intervening target before the missile is spawned");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void clearCandidateWinsAndBlockedFallbackUsesFarthestContact(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var missile = s.missile();
            var end = s.origin.add(12, 0, 0);
            var plans = FlySwatterTrajectory.candidates(s.origin, new Vec3(1, 0, 0), end, 0);
            s.block(BlockPos.containing(plans.get(0).curve().position(0.5)), Blocks.STONE.defaultBlockState());
            h.assertTrue(FlySwatterTrajectory.firstObstruction(h.getLevel(), missile, plans.get(0).curve()) != null,
                    "Fixture must obstruct the first preferred curve");
            var selected = FlySwatterTrajectory.select(h.getLevel(), missile, s.origin, new Vec3(1, 0, 0), end, 0);
            h.assertTrue(FlySwatterTrajectory.firstObstruction(h.getLevel(), missile, selected.curve()) == null,
                    "A clear candidate must win over an obstructed preferred curve");
            for (int y = -4; y <= 4; y++) for (int z = -4; z <= 4; z++) {
                s.block(BlockPos.containing(s.origin.add(5, y, z)), Blocks.STONE.defaultBlockState());
            }
            double farthest = -1;
            for (var plan : plans) {
                var hit = FlySwatterTrajectory.firstObstruction(h.getLevel(), missile, plan.curve());
                h.assertTrue(hit != null && !hit.unloaded(), "Wall must obstruct every candidate in loaded chunks");
                farthest = Math.max(farthest, s.origin.distanceToSqr(Objects.requireNonNull(hit).position()));
            }
            selected = FlySwatterTrajectory.select(h.getLevel(), missile, s.origin, new Vec3(1, 0, 0), end, 0);
            var hit = FlySwatterTrajectory.firstObstruction(h.getLevel(), missile, selected.curve());
            h.assertTrue(Math.abs(s.origin.distanceToSqr(Objects.requireNonNull(hit).position()) - farthest) < 1.0e-6,
                    "Blocked fallback must maximize distance from the launch position, not curve length");
            double length = 0;
            var previous = selected.curve().start();
            for (var sample : FlySwatterTrajectory.samples(selected.curve())) {
                length += previous.distanceTo(sample.position());
                previous = sample.position();
            }
            h.assertTrue(selected.arrivalTicks() == Mth.clamp((int) Math.ceil(length / 2.2), 4, 180),
                    "Blocked fallback must resume sampling to obtain the full flight length exactly once");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void newlyBlockedFlightHitsWithoutChoosingAnotherCurve(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.target(s.origin.add(12, 0, 0));
            var missile = s.missile();
            var plan = FlySwatterTrajectory.select(h.getLevel(), missile, s.origin, new Vec3(1, 0, 0),
                    target.getBoundingBox().getCenter(), 0);
            missile.launch(s.origin, s.origin, new Vec3(1, 0, 0), target, 0);
            // 発射時には通れた曲線を塞いでも、飛行中の経路探索はしない。
            s.block(BlockPos.containing(plan.curve().position(0.5)), Blocks.STONE.defaultBlockState());
            var expected = FlySwatterTrajectory.firstObstruction(h.getLevel(), missile, plan.curve());
            h.assertTrue(expected != null, "Fixture must block the selected flight");
            for (int tick = 0; tick < missile.getArrivalTicks() && !missile.isRemoved() && !missile.isBursting(); tick++) s.tick(missile);
            h.assertTrue(missile.isBursting(), "A newly blocked selected route must impact instead of replanning");
            h.assertTrue(missile.position().distanceTo(Objects.requireNonNull(expected).position()) < 0.08,
                    "Runtime impact must agree with preflight collision sampling");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void muzzleCannotSkipWallAndEntityImpactStopsBeforeWall(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.target(s.origin.add(12, 0, 0));
            var missile = s.missile();
            var block = BlockPos.containing(s.origin.add(0.6, 0, 0));
            s.block(block, Blocks.STONE.defaultBlockState());
            missile.launch(s.origin, s.origin.add(1, 0, 0), new Vec3(1, 0, 0), target, 0);
            h.assertTrue(missile.isBursting() && missile.position().x < s.origin.x + 1,
                    "Muzzle lead must impact a wall instead of spawning through it");
        }
        try (var s = new Scene(h)) {
            var target = s.target(s.origin.add(8, 0, 0));
            s.block(BlockPos.containing(s.origin.add(5, 0, 0)), Blocks.STONE.defaultBlockState());
            var missile = s.missile();
            var selected = FlySwatterTrajectory.select(h.getLevel(), missile, s.origin, new Vec3(1, 0, 0),
                    target.getBoundingBox().getCenter(), 0);
            // 展開幅を変えても、弾の進路上の敵を判定するfixtureにする。
            var obstacle = s.target(selected.curve().position(0.3).add(0, -0.975, 0));
            missile.launch(s.origin, s.origin, new Vec3(1, 0, 0), target, 0);
            float health = obstacle.getHealth();
            for (int tick = 0; tick < missile.getArrivalTicks() && !missile.isRemoved() && !missile.isBursting(); tick++) s.tick(missile);
            h.assertTrue(missile.isBursting() && obstacle.getHealth() < health && missile.position().x < s.origin.x + 5,
                    "An intervening combat entity must impact before a later block");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void targetLossKeepsLastMovementAndLifetimeAndAntiMagicTerminate(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.target(s.origin.add(12, 0, 0));
            var missile = s.missile();
            missile.launch(s.origin, s.origin, new Vec3(1, 0, 0), target, 0);
            s.tick(missile);
            var position = missile.position();
            var movement = missile.getDeltaMovement();
            target.discard();
            s.tick(missile);
            h.assertTrue(missile.position().distanceTo(position.add(movement)) < 1.0e-6,
                    "Lost targets must preserve the last real displacement instead of picking another target");
            missile.tickCount = 199;
            s.tick(missile);
            h.assertTrue(missile.isRemoved(), "Missile must expire at its existing two hundred tick lifetime");
            var countered = s.missile();
            countered.onAntiMagic(MagicData.getPlayerMagicData(s.owner));
            h.assertTrue(countered.isRemoved(), "AntiMagic must still discard missiles");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void highTargetControlsPitchEvenUnderRoofAndPitchRemainsFixed(GameTestHelper h) {
        try (var s = new Scene(h)) {
            for (double height : new double[]{7.99, 8, 12}) {
                var launcher = s.launcher();
                // 判定に使う発射口と照準点の差を正確に合わせる。
                var muzzle = launcher.getStandbyPosition().add(launcher.getLookAngle());
                var target = s.target(muzzle.add(8, height - 0.975, 0));
                double centerOffset = target.getBoundingBox().getCenter().y - target.getY();
                target.setPos(muzzle.add(8, height - centerOffset, 0));
                var ground = s.target(s.origin.add(8, 0, 0));
                launcher.addLockOnTarget(ground);
                launcher.addLockOnTarget(target);
                s.block(BlockPos.containing(muzzle.add(0, 3, 0)), Blocks.STONE.defaultBlockState());
                launcher.startFiring(h.getLevel(), s.owner);
                for (int tick = 0; tick < 9; tick++) launcher.tickOnServer(h.getLevel());
                target.setPos(s.origin.add(8, 0, 0));
                launcher.tickOnServer(h.getLevel());
                float expected = height >= 8 ? -60 : 0;
                h.assertTrue(Math.abs(launcher.getXRot() - expected) < 1.0e-5,
                        "Pitch must depend on the initial eight block boundary, not roof or later target movement: " + height);
                launcher.discard();
                ground.discard(); target.discard();
            }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void unloadedRouteDoesNotCreateChunkAndDiscardsWithoutImpact(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var level = h.getLevel();
            var origin = new Vec3(30000000 - 128, s.origin.y, 30000000 - 128);
            var pos = BlockPos.containing(origin);
            h.assertFalse(level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4), "Remote fixture chunk must start unloaded");
            var missile = s.missile();
            missile.setPos(origin);
            missile.setDeltaMovement(new Vec3(1, 0, 0));
            var curve = new LockOnRayCurve(origin, origin.add(1, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 0));
            var obstruction = FlySwatterTrajectory.firstObstruction(level, missile, curve);
            h.assertTrue(obstruction != null && obstruction.unloaded() && obstruction.hit() == null,
                    "Unloaded space must not be treated as air or as a physical impact");
            s.tick(missile);
            h.assertTrue(missile.isRemoved(), "Flying into an unloaded chunk must discard the missile");
            h.assertFalse(level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4), "Trajectory evaluation and movement must not create the remote chunk");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void trailCodecKeepsClippedCurveAndPatternsVaryBetweenShots(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var first = FlySwatterTrajectory.candidates(s.origin, new Vec3(1, 0, 0), s.origin.add(12, 0, 0), 0).get(0);
            var next = FlySwatterTrajectory.candidates(s.origin, new Vec3(1, 0, 0), s.origin.add(12, 0, 0), 1).get(0);
            h.assertTrue(first.pattern() != next.pattern() && !first.curve().startTangent().equals(next.curve().startTangent()),
                    "Adjacent missiles must prefer distinct spread trajectories");
            var curve = first.curve().prefix(0.37);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                FlySwatterTrailPacket.encode(new FlySwatterTrailPacket(curve), buffer);
                var copy = FlySwatterTrailPacket.decode(buffer);
                h.assertTrue(copy.curve().equals(curve), "Trail codec must preserve the exact clipped visible curve");
            } finally { buffer.release(); }
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void fluidDoesNotBlockOrDisplaceCurvedFlight(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.target(s.origin.add(12, 0, 0));
            var missile = s.missile();
            var plan = FlySwatterTrajectory.select(h.getLevel(), missile, s.origin, new Vec3(1, 0, 0),
                    target.getBoundingBox().getCenter(), 0);
            s.block(BlockPos.containing(s.origin), Blocks.WATER.defaultBlockState());
            s.block(BlockPos.containing(plan.curve().position(1.0 / plan.arrivalTicks())), Blocks.WATER.defaultBlockState());
            missile.launch(s.origin, s.origin, new Vec3(1, 0, 0), target, 0);
            s.tick(missile);
            h.assertTrue(!missile.isRemoved()
                            && missile.position().distanceTo(plan.curve().position(1.25 / plan.arrivalTicks())) < 1.0e-6,
                    "Fluid must neither obstruct nor push the missile off its selected curve");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void scaffoldCollisionUsesFlightAltitudeInsteadOfLaunchAltitude(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var missile = s.missile();
            missile.setPos(s.origin);
            var pos = BlockPos.containing(s.origin.add(0, 1, 0));
            s.block(pos, Blocks.SCAFFOLDING.defaultBlockState());
            var from = Vec3.atBottomCenterOf(pos).add(0, 1.3, 0);
            var to = from.add(0, -0.4, 0);
            var hit = FlySwatterTrajectory.traceSegment(h.getLevel(), missile, from, to);
            h.assertTrue(hit != null && hit.hit() != null && !hit.unloaded(),
                    "Preflight collision must use the sampled altitude for scaffold top surfaces");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void wideSpreadHasEighteenCandidatesAndPreservesOriginalFallbacks(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var end = s.origin.add(32, 0, 0);
            var candidates = FlySwatterTrajectory.candidates(s.origin, new Vec3(1, 0, 0), end, 0);
            h.assertTrue(candidates.size() == 18, "Flight must provide eight wide, eight original, and two direct candidates");
            var wide = candidates.get(0);
            var original = candidates.get(8);
            double wideOffset = Math.abs(wide.curve().position(1.0 / 3).z - s.origin.z);
            double originalOffset = Math.abs(original.curve().position(1.0 / 3).z - s.origin.z);
            h.assertTrue(wideOffset > originalOffset * 2.5, "Wide missile trajectories must visibly fan out beyond original curves");
            h.assertTrue(original.curve().startTangent().length() <= 24,
                    "Narrow fallback must retain the original tangent cap");
            var missile = s.missile();
            var selected = FlySwatterTrajectory.select(h.getLevel(), missile, s.origin, new Vec3(1, 0, 0), end, 0);
            h.assertTrue(selected.curve().equals(wide.curve()), "Open air must prefer the first wide candidate");
            var near = FlySwatterTrajectory.candidates(s.origin, new Vec3(1, 0, 0), s.origin.add(2, 0, 0), 0);
            h.assertTrue(near.get(0).curve().startTangent().length() < near.get(8).curve().startTangent().length() * 1.2,
                    "Very close targets must suppress the wide launch multiplier");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void narrowCorridorFallsBackToOriginalSpread(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var origin = s.origin.add(0, 0.5, 0.5);
            var end = origin.add(20, 0, 0);
            // 幅・高さ2ブロックの通路。大きな8軌道は塞がり、従来の斜め展開は通れる。
            for (int x = 0; x <= 20; x++) for (int y = -2; y <= 1; y++) for (int z = -2; z <= 1; z++) {
                if (y == -2 || y == 1 || z == -2 || z == 1) {
                    s.block(BlockPos.containing(origin.add(x, y, z)), Blocks.STONE.defaultBlockState());
                }
            }
            var missile = s.missile();
            var candidates = FlySwatterTrajectory.candidates(origin, new Vec3(1, 0, 0), end, 0);
            for (int i = 0; i < 8; i++) {
                h.assertTrue(FlySwatterTrajectory.firstObstruction(h.getLevel(), missile, candidates.get(i).curve()) != null,
                        "Narrow corridor must obstruct every wide candidate");
            }
            var selected = FlySwatterTrajectory.select(h.getLevel(), missile, origin, new Vec3(1, 0, 0), end, 0);
            h.assertTrue(selected.pattern() >= 8 && selected.pattern() < 16
                            && FlySwatterTrajectory.firstObstruction(h.getLevel(), missile, selected.curve()) == null,
                    "Original spread must remain available after wide candidates fail");
        }
        h.succeed();
    }

    private static final class Scene implements AutoCloseable {
        final GameTestHelper helper;
        final Vec3 origin;
        final FakePlayer owner;
        final List<Entity> entities = new ArrayList<>();
        final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();

        Scene(GameTestHelper helper) {
            this.helper = helper;
            // 1.20.1のGameTest原点は地下のため、射線と爆風が地形に遮られない上空へ置く。
            origin = helper.absoluteVec(new Vec3(2.5, 260.5, 2.5));
            owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "fly_trajectory_test"));
            owner.setPos(origin.add(-2, -1, 0)); owner.setYRot(-90); owner.setXRot(0);
            owner.setYHeadRot(-90); owner.yHeadRotO = -90;
            owner.setNoGravity(true);
            helper.getLevel().addFreshEntity(owner); entities.add(owner);
        }

        Zombie target(Vec3 position) {
            var target = Objects.requireNonNull(EntityType.ZOMBIE.create(helper.getLevel()), "Failed to create trajectory target");
            target.setPos(position); target.setNoGravity(true); target.setNoAi(true);
            helper.getLevel().addFreshEntity(target); entities.add(target);
            return target;
        }

        FlySwatterProjectileEntity missile() {
            var missile = new FlySwatterProjectileEntity(EntityRegistry.FLY_SWATTER_PROJECTILE.get(), helper.getLevel(), owner);
            missile.setDamage(1); missile.setRadius(0);
            entities.add(missile);
            return missile;
        }

        FlySwatterLauncherEntity launcher() {
            var launcher = new FlySwatterLauncherEntity(EntityRegistry.FLY_SWATTER_LAUNCHER.get(), helper.getLevel(), owner);
            entities.add(launcher);
            return launcher;
        }

        void block(BlockPos pos, BlockState state) {
            blocks.putIfAbsent(pos, helper.getLevel().getBlockState(pos));
            helper.getLevel().setBlockAndUpdate(pos, state);
        }

        void tick(FlySwatterProjectileEntity missile) {
            // ServerLevelが通常行うtickCountの更新も、手動tickで再現する。
            ++missile.tickCount;
            missile.tick();
        }

        @Override public void close() {
            helper.getLevel().getEntitiesOfClass(FlySwatterProjectileEntity.class, owner.getBoundingBox().inflate(150))
                    .stream().filter(e -> e.getOwner() == owner).forEach(Entity::discard);
            entities.forEach(Entity::discard);
            blocks.forEach((pos, state) -> helper.getLevel().setBlockAndUpdate(pos, state));
        }
    }
}
