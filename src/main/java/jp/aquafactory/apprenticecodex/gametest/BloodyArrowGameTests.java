package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodyArrowEntity;
import jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodyArrowOrbEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BloodyArrowGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.bloody_arrow";

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void flightDelaysGravityAndExpires(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var origin = helper.absoluteVec(new Vec3(2, 180, 2));
            var arrow = scene.arrow(origin, new Vec3(1, 0, 0), 1, 3);
            for (int i = 0; i < 10; i++) step(arrow);
            helper.assertTrue(arrow.position().distanceTo(origin.add(25, 0, 0)) < 1.0e-6,
                    "Arrow must travel 25 blocks without falling in its first ten ticks");
            step(arrow);
            helper.assertTrue(Math.abs(arrow.getY() - origin.y) < 1.0e-6
                            && Math.abs(arrow.getDeltaMovement().y + 0.05) < 1.0e-6,
                    "Gravity must begin after ten complete ticks without gravity");
            step(arrow);
            helper.assertTrue(Math.abs(arrow.getY() - origin.y + 0.05) < 1.0e-6,
                    "The next movement must include the first gravity step");
            // 寿命だけを検証する区間では地形や未読込チャンクへ飛ばさない。
            arrow.setPos(origin);
            arrow.setDeltaMovement(Vec3.ZERO);
            arrow.setNoGravity(true);
            for (int i = arrow.tickCount; i < 199; i++) step(arrow);
            helper.assertFalse(arrow.isRemoved(), "Arrow must remain until its lifetime boundary");
            step(arrow);
            helper.assertTrue(arrow.isRemoved() && !arrow.shouldBeSaved(), "Arrow must expire at 200 ticks and never save");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void waterFlowAndLavaDoNotChangeFlight(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var origin = helper.absoluteVec(new Vec3(1.5, 20.3, 2.5));
            for (var state : List.of(Blocks.AIR.defaultBlockState(), Blocks.WATER.defaultBlockState(),
                    Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3), Blocks.LAVA.defaultBlockState())) {
                // 水と溶岩を逐次置換すると隣接更新で石になる。各条件を空の区間から始める。
                for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, 20, 2), Blocks.AIR);
                for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, 20, 2), state);
                var arrow = scene.arrow(origin, new Vec3(1, 0, 0), 1, 3);
                step(arrow);
                helper.assertTrue(arrow.position().distanceTo(origin.add(2.5, 0, 0)) < 1.0e-6
                                && arrow.getDeltaMovement().distanceTo(new Vec3(2.5, 0, 0)) < 1.0e-6,
                        "Fluid motion mismatch: state=" + state + ", position=" + arrow.position()
                                + ", velocity=" + arrow.getDeltaMovement() + ", removed=" + arrow.isRemoved());
                arrow.discard();
            }
            for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, 20, 2), Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void successfulHitsDropOrbsAndKillsDoubleThem(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            for (int level : List.of(1, 2, 9, 10)) {
                int count = 3 + level / 2;
                var target = scene.zombie(new Vec3(3, 20, 3));
                var arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 20.8, 3)), new Vec3(1, 0, 0), 2, count);
                step(arrow);
                helper.assertTrue(target.getHealth() < 20 && arrow.isRemoved() && scene.orbs().size() == count,
                        "A successful living hit must drop the configured count exactly once");
                step(arrow);
                helper.assertTrue(scene.orbs().size() == count, "Removed arrows must not repeat their hit");
                scene.clearOrbs();
                target.discard();

                target = scene.zombie(new Vec3(3, 20, 3));
                target.setHealth(1);
                arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 20.8, 3)), new Vec3(1, 0, 0), 10, count);
                step(arrow);
                helper.assertTrue(!target.isAlive() && scene.orbs().size() == count * 2,
                        "A lethal hit must double the configured count");
                scene.clearOrbs();
                target.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void invulnerabilityTotemAndCrystalHaveDistinctOutcomes(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var target = scene.zombie(new Vec3(3, 20, 3));
            target.setInvulnerable(true);
            var arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 20.8, 3)), new Vec3(1, 0, 0), 10, 3);
            step(arrow);
            helper.assertTrue(arrow.isRemoved() && scene.orbs().isEmpty(), "Rejected damage must not create orbs");
            target.discard();
            target = scene.zombie(new Vec3(3, 20, 3));
            target.setHealth(1);
            target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 20.8, 3)), new Vec3(1, 0, 0), 10, 3);
            step(arrow);
            helper.assertTrue(target.isAlive() && target.getOffhandItem().isEmpty() && scene.orbs().size() == 3,
                    "Totem survival must keep the normal orb count");
            scene.clearOrbs();
            target.discard();
            var crystal = EntityType.END_CRYSTAL.create(helper.getLevel());
            crystal.setPos(helper.absoluteVec(new Vec3(3, 20, 3)));
            scene.add(crystal);
            // 通常の地形から離れた高度で、実際の破壊経路を確認する。
            crystal.setInvulnerable(false);
            arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 20.8, 3)), new Vec3(1, 0, 0), 2, 3);
            step(arrow);
            helper.assertTrue(crystal.isRemoved() && scene.orbs().isEmpty(),
                    "End crystals must remain damageable without producing blood orbs");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void terrainStopsFastShotsBeforeTargets(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            helper.setBlock(new BlockPos(2, 20, 3), Blocks.STONE);
            var target = scene.zombie(new Vec3(3.5, 20, 3.5));
            for (double x : List.of(1.99, 2.5)) {
                var arrow = scene.arrow(helper.absoluteVec(new Vec3(x, 20.5, 3.5)), new Vec3(1, 0, 0), 10, 3);
                step(arrow);
                helper.assertTrue(arrow.isRemoved() && target.getHealth() == 20 && scene.orbs().isEmpty(),
                        "Near-wall and inside-wall shots must not hit targets behind terrain");
            }
            helper.setBlock(new BlockPos(2, 20, 3), Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void playersCollectAtFullHealthButOtherEntitiesDoNot(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
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
            wolf.setPos(helper.absoluteVec(new Vec3(3, 20, 3)));
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
        try (var scene = new Scene(helper)) {
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
                scene.owner.setPos(helper.absoluteVec(new Vec3(1, 25, 1)));
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
        try (var scene = new Scene(helper)) {
            scene.owner.setPos(helper.absoluteVec(new Vec3(2.4, 20, 2.5)));
            helper.setBlock(new BlockPos(2, 20, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(2, 21, 2), Blocks.STONE);
            var orb = scene.orb(helper.absoluteVec(new Vec3(1.8, 20.5, 2.5)), 2);
            step(orb);
            helper.assertFalse(orb.isRemoved(), "Nearby owners behind a wall must not collect");
            helper.setBlock(new BlockPos(2, 20, 2), Blocks.AIR);
            helper.setBlock(new BlockPos(2, 21, 2), Blocks.AIR);
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
            orb = scene.orb(helper.absoluteVec(new Vec3(3, 20, 3)), 2);
            orb.onAntiMagic(null);
            var arrow = scene.arrow(helper.absoluteVec(new Vec3(3, 20, 3)), new Vec3(1, 0, 0), 2, 3);
            arrow.onAntiMagic(null);
            helper.assertTrue(orb.isRemoved() && arrow.isRemoved(), "Anti-magic must remove both entities");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void castingUsesExistingSpellValues(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            scene.owner.setPos(helper.absoluteVec(new Vec3(0.8, 20, 3)));
            scene.owner.setYRot(-90);
            scene.owner.setXRot(0);
            var spell = SpellRegistry.BLOODY_ARROW.get();
            var info = spell.getUniqueInfo(10, scene.owner);
            float damage = Float.parseFloat(((TranslatableContents) info.get(0).getContents()).getArgs()[0].toString());
            float healing = Float.parseFloat(((TranslatableContents) info.get(2).getContents()).getArgs()[0].toString());
            var target = scene.zombie(new Vec3(3, 20, 3));
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            target.setHealth(200);
            spell.onCast(helper.getLevel(), 10, scene.owner, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(scene.owner));
            var arrows = helper.getLevel().getEntitiesOfClass(BloodyArrowEntity.class, scene.owner.getBoundingBox().inflate(1));
            helper.assertTrue(arrows.size() == 1, "Casting must spawn exactly one arrow");
            var arrow = arrows.getFirst();
            scene.entities.add(arrow);
            helper.assertTrue(arrow.position().distanceTo(scene.owner.getEyePosition()) < 1.0e-6,
                    "Casting must start at the eye without skipping nearby terrain");
            step(arrow);
            helper.assertTrue(Math.abs(200 - target.getHealth() - damage) < 0.02 && scene.orbs().size() == 8,
                    "Casting must use existing damage and level-dependent count");
            var orbs = scene.orbs();
            var orb = orbs.getFirst();
            orbs.stream().skip(1).forEach(Entity::discard);
            scene.owner.setHealth(5);
            orb.setPos(scene.owner.position());
            orb.scatter(scene.owner.position(), scene.owner.position());
            orb.setNoGravity(true);
            orb.setDeltaMovement(Vec3.ZERO);
            step(orb);
            helper.assertTrue(Math.abs(scene.owner.getHealth() - 5 - healing) < 0.02,
                    "Orbs must use the healing amount advertised by the spell");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void nearestCandidateWinsAndSummonsCanCollect(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var summon = new TestSummon(helper.getLevel(), scene.owner);
            summon.setNoAi(true);
            summon.setNoGravity(true);
            summon.setPos(helper.absoluteVec(new Vec3(3, 20, 3)));
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

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 80)
    public static void normalServerTicksHoverOrbsWithoutHopperPickup(GameTestHelper helper) {
        var scene = new Scene(helper);
        var local = new BlockPos(3, 19, 3);
        helper.setBlock(local, Blocks.HOPPER);
        var orb = new BloodyArrowOrbEntity(EntityRegistry.BLOODY_ARROW_ORB.get(), helper.getLevel());
        orb.spawn(scene.owner, helper.absoluteVec(new Vec3(3.5, 22, 3.5)), 2);
        var anchor = jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodOrbPlacement.findAnchor(scene.owner, orb.position());
        helper.assertTrue(anchor != null, "Hopper must provide a reachable hover anchor");
        orb.scatter(anchor, anchor);
        orb.setDeltaMovement(Vec3.ZERO);
        scene.add(orb);
        var arrow = scene.arrow(helper.absoluteVec(new Vec3(1, 50, 1)), new Vec3(0, 1, 0), 2, 3);
        helper.runAfterDelay(30, () -> {
            try (scene) {
                var hopper = (net.minecraft.world.level.block.entity.HopperBlockEntity) helper.getBlockEntity(local);
                helper.assertTrue(!orb.isRemoved() && orb.position().distanceTo(anchor) < 0.09 && orb.isNoGravity(),
                        "Normal server ticks must guide the orb to a stable hover above the hopper");
                helper.assertTrue(hopper.isEmpty(), "Hoppers must not collect blood orbs");
                helper.assertTrue(arrow.tickCount >= 20 && arrow.getDeltaMovement().y < 2.5,
                        "Normal server ticks must start projectile gravity");
            } finally {
                helper.setBlock(local, Blocks.AIR);
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void cancelledImpactContinuesWithoutDamage(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var target = scene.zombie(new Vec3(3, 20, 3));
            target.setBaby(true);
            var arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 20.4, 3)), new Vec3(1, 0, 0), 10, 3);
            Consumer<net.neoforged.neoforge.event.entity.ProjectileImpactEvent> listener = event -> {
                if (event.getProjectile() == arrow) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                step(arrow);
                helper.assertTrue(!arrow.isRemoved() && target.getHealth() == 20 && scene.orbs().isEmpty(),
                        "Cancelled impacts must not damage or create orbs");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
            arrow.discard();
            var hit = scene.arrow(helper.absoluteVec(new Vec3(0.8, 20.4, 3)), new Vec3(1, 0, 0), 2, 3);
            step(hit);
            helper.assertTrue(hit.isRemoved() && target.getHealth() < 20 && scene.orbs().size() == 3,
                    "An uncancelled fast arrow must hit a small target between tick positions");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void scatterUsesWideReachableGroundAndAvoidsWallsAndPits(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var saved = new java.util.LinkedHashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
            try {
                // 広い散布範囲は通常テストの高度から離し、全ブロックを終了時に復元する。
                for (var local : BlockPos.betweenClosed(new BlockPos(-5, 40, -5), new BlockPos(9, 43, 9))) {
                    var absolute = helper.absolutePos(local);
                    saved.put(absolute.immutable(), helper.getLevel().getBlockState(absolute));
                    helper.getLevel().setBlockAndUpdate(absolute,
                            local.getY() == 40 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                var impact = helper.absoluteVec(new Vec3(2.5, 55, 2.5));
                var anchor = jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodOrbPlacement.findAnchor(scene.owner, impact);
                helper.assertTrue(anchor != null && Math.abs(anchor.y - helper.absoluteVec(new Vec3(0, 41.65, 0)).y) < 0.001,
                        "Airborne impacts must find ground and hover 0.65 blocks above it");
                var points = new ArrayList<Vec3>();
                for (int i = 0; i < 8; i++) {
                    var destination = jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodOrbPlacement.destination(
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
                for (int y = 41; y <= 43; y++) helper.setBlock(new BlockPos(4, y, 2), Blocks.STONE);
                var blocked = jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodOrbPlacement.destination(scene.owner, anchor, 0, 4);
                helper.assertTrue(blocked.x < helper.absoluteVec(new Vec3(4, 0, 0)).x,
                        "Scatter must not choose a destination behind a wall");
                for (int y = 41; y <= 43; y++) helper.setBlock(new BlockPos(4, y, 2), Blocks.AIR);
                helper.setBlock(new BlockPos(4, 40, 2), Blocks.AIR);
                var pit = jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodOrbPlacement.destination(scene.owner, anchor, 0, 4);
                helper.assertTrue(pit.x < helper.absoluteVec(new Vec3(4, 0, 0)).x,
                        "Scatter must not cross a pit to choose distant ground");
                scene.owner.setPos(anchor);
                var fallback = jp.aquafactory.apprenticecodex.spell.bloodyarrow.BloodOrbPlacement.findAnchor(
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
    public static void orbLifetimesVaryAndPickupSoundsAreCoalesced(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var lifetimes = new java.util.HashSet<Integer>();
            var sounds = new AtomicInteger();
            Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition> listener = event -> {
                if (event.getSound() != null && event.getSound().value() == net.minecraft.sounds.SoundEvents.ITEM_PICKUP) sounds.incrementAndGet();
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                for (int i = 0; i < 32; i++) {
                    var orb = scene.orb(scene.owner.position(), 2);
                    lifetimes.add(orb.lifetimeTicks());
                    helper.assertTrue(orb.lifetimeTicks() >= 200 && orb.lifetimeTicks() <= 240,
                            "Each orb must have an independent lifetime between 200 and 240 ticks");
                    step(orb);
                    helper.assertTrue(orb.isRemoved(), "Full-health players must still collect the magical orbs");
                }
                helper.assertTrue(lifetimes.size() > 1, "Orbs must not all expire at the same time");
                helper.assertTrue(sounds.get() == 1, "Simultaneous pickups must play only one item pickup sound");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
        }
        helper.succeed();
    }

    private static final class TestSummon extends Zombie implements io.redspace.ironsspellbooks.entity.mobs.IMagicSummon {
        private Entity summoner;

        private TestSummon(net.minecraft.world.level.Level level, Entity summoner) {
            super(EntityType.ZOMBIE, level);
            this.summoner = summoner;
        }

        @Override public Entity getSummoner() { return summoner; }
        @Override public void onUnSummon() { discard(); }
    }

    private static void step(Entity entity) {
        // 通常はServerLevelがtick()の前に加算する。手動tickでも同じ順序を再現する。
        entity.tickCount++;
        entity.tick();
    }

    private static final class Scene implements AutoCloseable {
        private final GameTestHelper helper;
        private final List<Entity> entities = new ArrayList<>();
        private final FakePlayer owner;

        private Scene(GameTestHelper helper) {
            this.helper = helper;
            owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "blood_test"));
            owner.setPos(helper.absoluteVec(new Vec3(1, 20, 1)));
            owner.setNoGravity(true);
            add(owner);
        }

        private void add(Entity entity) {
            entities.add(entity);
            helper.getLevel().addFreshEntity(entity);
        }

        private Zombie zombie(Vec3 local) {
            var target = EntityType.ZOMBIE.create(helper.getLevel());
            target.setNoAi(true);
            target.setNoGravity(true);
            target.getAttribute(Attributes.ARMOR).setBaseValue(0);
            target.setPos(helper.absoluteVec(local));
            add(target);
            return target;
        }

        private BloodyArrowEntity arrow(Vec3 origin, Vec3 direction, float damage, int count) {
            var arrow = new BloodyArrowEntity(EntityRegistry.BLOODY_ARROW.get(), helper.getLevel());
            arrow.launch(owner, origin, direction, damage, count, 2);
            add(arrow);
            return arrow;
        }

        private BloodyArrowOrbEntity orb(Vec3 position, float healing) {
            var orb = new BloodyArrowOrbEntity(EntityRegistry.BLOODY_ARROW_ORB.get(), helper.getLevel());
            orb.spawn(owner, position, healing);
            orb.setDeltaMovement(Vec3.ZERO);
            orb.setNoGravity(true);
            add(orb);
            return orb;
        }

        private List<BloodyArrowOrbEntity> orbs() {
            return helper.getLevel().getEntitiesOfClass(BloodyArrowOrbEntity.class,
                    new net.minecraft.world.phys.AABB(helper.absoluteVec(new Vec3(-1, 18, -1)),
                            helper.absoluteVec(new Vec3(6, 24, 6))));
        }

        private void clearOrbs() { orbs().forEach(Entity::discard); }

        @Override public void close() {
            clearOrbs();
            entities.forEach(Entity::discard);
        }
    }
}
