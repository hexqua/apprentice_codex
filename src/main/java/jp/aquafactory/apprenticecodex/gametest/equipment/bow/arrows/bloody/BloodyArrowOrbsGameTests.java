package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.bloody;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.gametest.equipment.bow.support.ArrowTestPositions;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodOrbPlacement;
import jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodyArrowOrbEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.bloody.BloodyArrowFixture.step;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BloodyArrowOrbsGameTests {
    private BloodyArrowOrbsGameTests() {
    }

    private static final String TEMPLATE = "gametest/arrow_combat";
    private static final String BATCH = "apprenticecodex.bloody_arrow";

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void playersCollectAtFullHealthButOtherEntitiesDoNot(GameTestHelper helper) {
        try (var scene = new BloodyArrowFixture(helper)) {
            var orb = scene.orb(scene.owner.position(), 2);
            var events = new AtomicInteger();
            Consumer<SpellHealEvent> listener = event -> {
                if (event.getTargetEntity() == scene.owner) events.incrementAndGet();
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                step(orb);
                step(orb);
                helper.assertTrue(orb.isRemoved() && events.get() == 1 && scene.owner.getHealth() == scene.owner.getMaxHealth(),
                        "Full-health players must consume one orb and emit one spell heal event");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
            var wolf = EntityType.WOLF.create(helper.getLevel());
            wolf.setOwnerUUID(scene.owner.getUUID());
            wolf.setTame(true, false);
            wolf.setNoAi(true);
            wolf.setNoGravity(true);
            wolf.setPos(ArrowTestPositions.absolute(helper, new Vec3(3, 20, 3)));
            wolf.setHealth(wolf.getMaxHealth());
            scene.add(wolf);
            orb = scene.orb(wolf.position(), 2);
            step(orb);
            helper.assertFalse(orb.isRemoved(), "Full-health pets must leave the orb available");
            wolf.setHealth(wolf.getMaxHealth() - 4);
            step(orb);
            helper.assertTrue(orb.isRemoved() && Math.abs(wolf.getHealth() - (wolf.getMaxHealth() - 2)) < 1.0e-5,
                    "Injured owned pets must recover the requested health");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void collectionUsesAlliesNotAttackProtection(GameTestHelper helper) {
        try (var scene = new BloodyArrowFixture(helper)) {
            var target = scene.zombie(new Vec3(3, 20, 3));
            target.setHealth(10);
            var orb = scene.orb(target.position(), 2);
            step(orb);
            helper.assertFalse(orb.isRemoved(), "Unrelated injured entities must not collect");
            var scoreboard = helper.getLevel().getScoreboard();
            var team = scoreboard.addPlayerTeam("ba_" + UUID.randomUUID().toString().substring(0, 8));
            try {
                scoreboard.addPlayerToTeam(scene.owner.getScoreboardName(), team);
                scoreboard.addPlayerToTeam(target.getScoreboardName(), team);
                team.setAllowFriendlyFire(true);
                step(orb);
                helper.assertTrue(orb.isRemoved() && target.getHealth() == 12,
                        "Allies must collect even when their team allows friendly fire");
                var spectator = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "blood_spectator"));
                spectator.setPos(scene.owner.position());
                scene.add(spectator);
                scoreboard.addPlayerToTeam(spectator.getScoreboardName(), team);
                spectator.setGameMode(GameType.SPECTATOR);
                scene.owner.setPos(ArrowTestPositions.absolute(helper, new Vec3(1, 25, 1)));
                orb = scene.orb(spectator.position(), 2);
                step(orb);
                helper.assertFalse(orb.isRemoved(), "Spectators must not consume allied orbs");
            } finally {
                scoreboard.removePlayerTeam(team);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void orbsRespectWallsOwnersLifetimeAndAntiMagic(GameTestHelper helper) {
        try (var scene = new BloodyArrowFixture(helper)) {
            scene.owner.setPos(ArrowTestPositions.absolute(helper, new Vec3(2.4, 20, 2.5)));
            ArrowTestPositions.setBlock(helper, new BlockPos(2, 20, 2), Blocks.STONE);
            ArrowTestPositions.setBlock(helper, new BlockPos(2, 21, 2), Blocks.STONE);
            var orb = scene.orb(ArrowTestPositions.absolute(helper, new Vec3(1.8, 20.5, 2.5)), 2);
            step(orb);
            helper.assertFalse(orb.isRemoved(), "Nearby owners behind a wall must not collect");
            ArrowTestPositions.setBlock(helper, new BlockPos(2, 20, 2), Blocks.AIR);
            ArrowTestPositions.setBlock(helper, new BlockPos(2, 21, 2), Blocks.AIR);
            orb.discard();
            orb = scene.orb(scene.owner.position(), 2);
            scene.owner.discard();
            step(orb);
            helper.assertFalse(orb.isRemoved(), "Missing casters must not allow collection");
            int expiration = orb.lifetimeTicks();
            helper.assertTrue(expiration >= 200 && expiration <= 240, "Orb lifetime must be between 200 and 240 ticks");
            for (int i = orb.tickCount; i < expiration - 1; i++) step(orb);
            helper.assertFalse(orb.isRemoved(), "Orbs must survive until their lifetime boundary");
            step(orb);
            helper.assertTrue(orb.isRemoved() && !orb.shouldBeSaved(), "Orbs must expire and never save");
            orb = scene.orb(ArrowTestPositions.absolute(helper, new Vec3(3, 20, 3)), 2);
            orb.onAntiMagic(null);
            var arrow = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(3, 20, 3)), new Vec3(1, 0, 0), 2, 3);
            arrow.onAntiMagic(null);
            helper.assertTrue(orb.isRemoved() && arrow.isRemoved(), "Anti-magic must remove both entities");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void nearestCandidateWinsAndSummonsCanCollect(GameTestHelper helper) {
        try (var scene = new BloodyArrowFixture(helper)) {
            var summon = new TestSummon(helper.getLevel(), scene.owner);
            summon.setNoAi(true);
            summon.setNoGravity(true);
            summon.setPos(ArrowTestPositions.absolute(helper, new Vec3(3, 20, 3)));
            summon.setHealth(10);
            scene.add(summon);
            var other = new TestSummon(helper.getLevel(), scene.owner);
            other.setNoAi(true);
            other.setNoGravity(true);
            other.setPos(summon.position().add(0.3, 0, 0));
            other.setHealth(10);
            scene.add(other);
            var orb = scene.orb(summon.position(), 2);
            step(orb);
            helper.assertTrue(summon.getHealth() == 12 && other.getHealth() == 10 && orb.isRemoved(),
                    "Only the nearest injured summon must consume the orb");
            orb = scene.orb(other.position(), 2);
            other.summoner = null;
            summon.discard();
            step(orb);
            helper.assertFalse(orb.isRemoved(), "Lost ownership must be respected at collection time");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/blood_arrow_hover", batch = BATCH, timeoutTicks = 80)
    public static void normalServerTicksHoverOrbsWithoutHopperPickup(GameTestHelper helper) {
        var scene = new BloodyArrowFixture(helper);
        var local = new BlockPos(3, 19, 3);
        ArrowTestPositions.setBlock(helper, local, Blocks.HOPPER);
        var orb = new BloodyArrowOrbEntity(EntityRegistry.BLOODY_ARROW_ORB.get(), helper.getLevel());
        orb.spawn(scene.owner, ArrowTestPositions.absolute(helper, new Vec3(3.5, 22, 3.5)), 2);
        var anchor = BloodOrbPlacement.findAnchor(scene.owner, orb.position());
        helper.assertTrue(anchor != null, "Hopper must provide a reachable hover anchor");
        orb.scatter(anchor, anchor);
        orb.setDeltaMovement(Vec3.ZERO);
        scene.add(orb);
        var arrow = scene.arrow(ArrowTestPositions.absolute(helper, new Vec3(1, 8, 1)), new Vec3(0, 1, 0), 2, 3);
        helper.runAfterDelay(30, () -> {
            try (scene) {
                var hopper = (HopperBlockEntity) helper.getBlockEntity(local.offset(12, 0, 12));
                helper.assertTrue(!orb.isRemoved() && orb.position().distanceTo(anchor) < 0.09 && orb.isNoGravity(),
                        "Normal server ticks must guide the orb to a stable hover above the hopper");
                helper.assertTrue(hopper.isEmpty(), "Hoppers must not collect blood orbs");
                helper.assertTrue(arrow.tickCount >= 20 && arrow.getDeltaMovement().y < 2.5,
                        "Normal server ticks must start projectile gravity");
            } finally {
                ArrowTestPositions.setBlock(helper, local, Blocks.AIR);
            }
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/blood_orb_scatter", batch = BATCH)
    public static void scatterUsesWideReachableGroundAndAvoidsWallsAndPits(GameTestHelper helper) {
        try (var scene = new BloodyArrowFixture(helper)) {
            var saved = new LinkedHashMap<BlockPos, BlockState>();
            try {
                // 散布に必要な床・壁・穴を専用structure内へ作り、終了時に元の状態へ戻す。
                for (var local : BlockPos.betweenClosed(new BlockPos(-5, 40, -5), new BlockPos(9, 43, 9))) {
                    var absolute = ArrowTestPositions.absolute(helper, local);
                    saved.put(absolute.immutable(), helper.getLevel().getBlockState(absolute));
                    helper.getLevel().setBlockAndUpdate(absolute,
                            local.getY() == 40 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                var impact = ArrowTestPositions.absolute(helper, new Vec3(2.5, 55, 2.5));
                var anchor = BloodOrbPlacement.findAnchor(scene.owner, impact);
                helper.assertTrue(anchor != null && Math.abs(anchor.y - ArrowTestPositions.absolute(helper, new Vec3(0, 41.65, 0)).y) < 0.001,
                        "Airborne impacts must find ground and hover 0.65 blocks above it");
                var points = new ArrayList<Vec3>();
                for (int i = 0; i < 8; i++) {
                    var destination = BloodOrbPlacement.destination(
                            scene.owner, anchor, i * Math.PI / 4, 4);
                    helper.assertTrue(Math.abs(destination.distanceTo(anchor) - 4) < 0.01,
                            "Open terrain must permit a four-block scatter in every direction");
                    points.add(destination);
                    var orb = scene.orb(anchor, 2);
                    orb.scatter(anchor, destination);
                    for (int tick = 0; tick < 35; tick++) step(orb);
                    helper.assertTrue(orb.position().distanceTo(destination) < 0.09 && orb.isNoGravity(),
                            "Orbs must spread out and settle without gravity");
                    var resting = orb.position();
                    for (int tick = 0; tick < 10; tick++) step(orb);
                    helper.assertTrue(orb.position().distanceTo(resting) < 0.001,
                            "Settled orbs must not drift out of reach");
                }
                helper.assertTrue(points.get(0).distanceTo(points.get(4)) > 7.9,
                        "Opposite orbs must require movement to collect");
                for (int y = 41; y <= 43; y++) ArrowTestPositions.setBlock(helper, new BlockPos(4, y, 2), Blocks.STONE);
                var blocked = BloodOrbPlacement.destination(scene.owner, anchor, 0, 4);
                helper.assertTrue(blocked.x < ArrowTestPositions.absolute(helper, new Vec3(4, 0, 0)).x,
                        "Scatter must not choose a destination behind a wall");
                for (int y = 41; y <= 43; y++) ArrowTestPositions.setBlock(helper, new BlockPos(4, y, 2), Blocks.AIR);
                ArrowTestPositions.setBlock(helper, new BlockPos(4, 40, 2), Blocks.AIR);
                var pit = BloodOrbPlacement.destination(scene.owner, anchor, 0, 4);
                helper.assertTrue(pit.x < ArrowTestPositions.absolute(helper, new Vec3(4, 0, 0)).x,
                        "Scatter must not cross a pit to choose distant ground");
                scene.owner.setPos(anchor);
                var fallback = BloodOrbPlacement.findAnchor(
                        scene.owner, impact.add(0, 100, 0));
                helper.assertTrue(fallback != null && fallback.distanceTo(anchor) < 0.01,
                        "Out-of-range airborne impacts must fall back to the caster's ground");
            } finally {
                saved.forEach((position, state) -> helper.getLevel().setBlockAndUpdate(position, state));
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void orbLifetimesStayWithinBoundsAndPickupSoundsAreCoalesced(GameTestHelper helper) {
        try (var scene = new BloodyArrowFixture(helper)) {
            var sounds = new AtomicInteger();
            Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
                if (event.getLevel() == helper.getLevel() && event.getPosition().distanceToSqr(scene.owner.position()) < 1.0e-8
                        && event.getSound() != null && event.getSound().value() == SoundEvents.ITEM_PICKUP)
                    sounds.incrementAndGet();
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                for (int i = 0; i < 32; i++) {
                    var orb = scene.orb(scene.owner.position(), 2);
                    helper.assertTrue(orb.lifetimeTicks() >= 200 && orb.lifetimeTicks() <= 240,
                            "Each orb must have an independent lifetime between 200 and 240 ticks");
                    step(orb);
                    helper.assertTrue(orb.isRemoved(), "Full-health players must still collect the magical orbs");
                }
                helper.assertTrue(sounds.get() == 1, "Simultaneous pickups must play only one item pickup sound");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
        }
        helper.succeed();
    }

    private static final class TestSummon extends Zombie implements IMagicSummon {
        private Entity summoner;

        private TestSummon(Level level, Entity summoner) {
            super(EntityType.ZOMBIE, level);
            this.summoner = summoner;
        }

        @Override
        public Entity getSummoner() {
            return summoner;
        }

        @Override
        public void onUnSummon() {
            discard();
        }
    }
}
