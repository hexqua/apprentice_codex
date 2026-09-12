package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.echoarrow.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.gametest.*;

import java.util.*;
import java.util.function.Consumer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoArrowGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String BATCH = "apprenticecodex.echo_arrow";

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void initialFlightMatchesBloodyArrowAndExpiresWithCore(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var pair = scene.pair(new Vec3(2, 120, 2), new Vec3(0, 1, 0), 7);
            Vec3 origin = pair.arrow.position();
            for (int i = 0; i < 10; i++) step(pair.arrow);
            helper.assertTrue(pair.arrow.position().distanceTo(origin.add(0, 25, 0)) < 1e-6,
                    "Initial arrow must travel ten ticks at 2.5 blocks per tick");
            step(pair.arrow);
            helper.assertTrue(Math.abs(pair.arrow.getDeltaMovement().y - 2.45) < 1e-6,
                    "Gravity must start after the eleventh movement, matching BloodyArrow");
            pair.arrow.setPos(origin);
            pair.arrow.setDeltaMovement(Vec3.ZERO);
            pair.arrow.setNoGravity(true);
            while (pair.arrow.tickCount < 199) step(pair.arrow);
            helper.assertFalse(pair.arrow.isRemoved(), "Initial arrow must survive until tick 200");
            step(pair.arrow);
            helper.assertTrue(pair.arrow.isRemoved() && pair.core.isRemoved(), "Expiration must also remove the waiting core");
            helper.assertFalse(pair.arrow.shouldBeSaved() || pair.core.shouldBeSaved(), "Echo entities must never persist");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 60)
    public static void actualTicksFireExactBatchesAtFixedImpactPoint(GameTestHelper helper) {
        var scene = new Scene(helper);
        var target = scene.zombie(new Vec3(3, 30, 3));
        target.setInvulnerable(true);
        var pair = scene.pair(new Vec3(.8, 31, 3), new Vec3(1, 0, 0), 7);
        step(pair.arrow);
        Vec3 impact = pair.arrow.position();
        long impactTime = helper.getLevel().getGameTime();
        helper.assertTrue(pair.arrow.isRemoved() && !pair.core.isRemoved(), "Rejected damage must still arm the core");
        helper.assertTrue(impact.y > target.getY() + .5, "Aim must use the projectile impact, not target feet");
        target.setPos(target.position().add(0, 5, 0));
        pair.core.acceptImpact(pair.arrow, impact.add(0, 10, 0));
        helper.runAfterDelay(20, () -> {
            try (scene) {
                var shots = scene.shots.stream().filter(s -> s.arrow != pair.arrow).toList();
                helper.assertTrue(shots.size() == 7, "Seven follow-up arrows must exclude the initial arrow: " + shots.size());
                for (int i = 0; i < shots.size(); i++) {
                    var shot = shots.get(i);
                    helper.assertTrue(shot.time == impactTime + 10 + i,
                            "Follow-up arrows must fire individually on consecutive server ticks: " + shot.time + ", impact=" + impactTime);
                    helper.assertTrue(shot.velocity.length() >= 2.4 && shot.velocity.length() <= 2.6,
                            "Follow-up speed must stay in the configured range");
                    helper.assertTrue(shot.velocity.normalize().distanceTo(impact.subtract(shot.origin).normalize()) < 1e-6,
                            "Every arrow must converge on the original impact after target movement");
                    helper.assertTrue(shot.arrow.isNoGravity(), "Follow-up arrows must remain gravity-free");
                }
                helper.assertTrue(shots.get(0).origin.distanceTo(shots.get(1).origin) > .1,
                        "Simultaneous arrows must have distinct spawn positions");
                helper.assertTrue(pair.core.isRemoved(), "Core must disappear after the final arrow");
                helper.assertTrue(shots.stream().anyMatch(s -> !s.arrow.isRemoved()), "Core completion must preserve fired arrows");
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void bothModesIgnoreArmorIframesAndKnockbackWithoutPiercing(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var target = scene.zombie(new Vec3(3, 30, 3));
            target.getAttribute(Attributes.ARMOR).setBaseValue(30);
            target.invulnerableTime = 20;
            var behind = scene.zombie(new Vec3(4, 30, 3));
            Vec3 before = target.getDeltaMovement();
            for (int i = 0; i < 4; i++) {
                var arrow = scene.arrow(scene.pos(new Vec3(.8, 31, 3)), new Vec3(1, 0, 0), i == 0, null);
                step(arrow);
                helper.assertTrue(arrow.isRemoved(), "Both arrow modes must stop at the first entity");
            }
            helper.assertTrue(Math.abs(target.getHealth() - 88) < .01, "Four arrows must each deal three damage despite armor and i-frames");
            helper.assertTrue(behind.getHealth() == 100, "The entity behind the first target must not be damaged");
            helper.assertTrue(target.getDeltaMovement().distanceTo(before) < 1e-6, "Arrows must not add knockback");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void counterspellAndRemovalRespectOneWayDependency(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            pair.arrow.onAntiMagic(null);
            helper.assertTrue(pair.core.isRemoved(), "Countering the initial arrow must remove its core");
            pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            pair.core.onAntiMagic(null);
            step(pair.arrow);
            helper.assertFalse(pair.arrow.isRemoved(), "Countering the core must not remove its initial arrow");
            pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            pair.arrow.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            helper.assertTrue(pair.core.isRemoved(), "Unloading the initial arrow must remove the waiting core");
            pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            pair.core.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            helper.assertFalse(pair.arrow.isRemoved(), "Unloading a core must not destroy an already fired arrow");
            var followup = scene.arrow(scene.pos(new Vec3(2, 30, 2)), new Vec3(0, 1, 0), false, null);
            followup.onAntiMagic(null);
            helper.assertTrue(followup.isRemoved(), "Follow-up arrows must also support anti-magic");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void realCounterspellRayCanSelectCore(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var pair = scene.pair(new Vec3(3, 31.62, 3), new Vec3(0, 1, 0), 7);
            pair.arrow.setPos(pair.arrow.position().add(0, 5, 0));
            scene.owner.setPos(scene.pos(new Vec3(.5, 30, 3)));
            scene.owner.setYRot(-90);
            scene.owner.setXRot(0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.COUNTERSPELL_SPELL.get();
            spell.onCast(helper.getLevel(), 1, scene.owner, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(scene.owner));
            helper.assertTrue(pair.core.isRemoved(), "The real Counterspell ray must select the visible core");
            helper.assertFalse(pair.arrow.isRemoved(), "Counterspell on the core must preserve the initial arrow");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void terrainStopsBothModesAndCancelledImpactDoesNotArm(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var wall = new BlockPos(3, 31, 3);
            helper.setBlock(wall, Blocks.STONE);
            try {
                for (boolean initial : new boolean[]{true, false}) {
                    var arrow = scene.arrow(scene.pos(new Vec3(.8, 31.5, 3.5)), new Vec3(1, 0, 0), initial, null);
                    step(arrow);
                    helper.assertTrue(arrow.isRemoved() && arrow.getX() <= scene.pos(new Vec3(3, 0, 0)).x + .001,
                            "Terrain must stop initial and follow-up arrows");
                }
            } finally {
                helper.setBlock(wall, Blocks.AIR);
            }
            var target = scene.zombie(new Vec3(3, 30, 3));
            var pair = scene.pair(new Vec3(.8, 31, 3), new Vec3(1, 0, 0), 7);
            Consumer<ProjectileImpactEvent> listener = event -> {
                if (event.getProjectile() == pair.arrow) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                step(pair.arrow);
                helper.assertTrue(!pair.arrow.isRemoved() && pair.core.startTime() < 0 && target.getHealth() == 100,
                        "Cancelled impacts must not arm the core or deal damage");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void ownerLossAndUnloadedDestinationEndEntities(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            scene.owner.setHealth(0);
            pair.core.tick();
            step(pair.arrow);
            helper.assertTrue(pair.core.isRemoved() && pair.arrow.isRemoved(), "Owner death must stop both entities");
        }
        try (var scene = new Scene(helper)) {
            var pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            scene.owner.remove(Entity.RemovalReason.CHANGED_DIMENSION);
            pair.core.tick();
            step(pair.arrow);
            helper.assertTrue(pair.core.isRemoved() && pair.arrow.isRemoved(), "Removed owners must not keep echoes active");
        }
        try (var scene = new Scene(helper)) {
            var pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            // GameTestの配置座標はランダムなので、巨大座標を足さず近隣の未読込chunkを使う。
            Vec3 movement = new Vec3(1024, 0, 0);
            if (pair.arrow.getX() > 0) movement = movement.scale(-1);
            helper.assertFalse(helper.getLevel().hasChunkAt(BlockPos.containing(pair.arrow.position().add(movement))),
                    "The test destination must be unloaded before flight");
            pair.arrow.setDeltaMovement(movement);
            step(pair.arrow);
            helper.assertTrue(pair.arrow.isRemoved() && pair.core.isRemoved(), "Unloaded destinations must end the shot without force-loading");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void castUsesExistingValuesAndImpactDuration(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var spell = SpellRegistry.ECHO_ARROW.get();
            var info = spell.getUniqueInfo(1, scene.owner);
            var args = ((TranslatableContents) info.get(1).getContents()).getArgs();
            int count = Integer.parseInt(args[0].toString());
            float duration = Float.parseFloat(((TranslatableContents) info.get(2).getContents()).getArgs()[0].toString().replace("s", ""));
            helper.assertTrue(Math.abs(duration - (10 + count - 1) / 20f) < .011,
                    "Tooltip must describe time from impact to the last shot");
            spell.onCast(helper.getLevel(), 1, scene.owner, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(scene.owner));
            helper.assertTrue(scene.shots.size() == 1 && scene.shots.getFirst().origin.distanceTo(scene.owner.getEyePosition()) < 1e-6,
                    "Casting must emit exactly one initial arrow at the eye");
            var cores = helper.getLevel().getEntitiesOfClass(EchoArrowCoreEntity.class, scene.owner.getBoundingBox().inflate(2));
            helper.assertTrue(cores.size() == 1 && cores.getFirst().totalCount() == count,
                    "The core must snapshot the advertised follow-up count");
            cores.forEach(Entity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 60)
    public static void counteringDuringVolleyStopsOnlyUnfiredArrows(GameTestHelper helper) {
        var scene = new Scene(helper);
        scene.zombie(new Vec3(3, 30, 3));
        var pair = scene.pair(new Vec3(.8, 31, 3), new Vec3(1, 0, 0), 15);
        step(pair.arrow);
        int[] emittedBeforeCounter = {0};
        helper.runAfterDelay(13, () -> {
            // 毎tick発射ではGameTest callbackとentity tickの実行順で境界本数が変わる。
            // 正確な発射時刻は別テストで確認し、ここでは途中停止後に増えないことを検証する。
            emittedBeforeCounter[0] = scene.shots.size();
            helper.assertTrue(emittedBeforeCounter[0] > 1 && emittedBeforeCounter[0] < 16,
                    "Counterspell must be applied after firing begins and before all arrows are emitted");
            pair.core.onAntiMagic(null);
        });
        helper.runAfterDelay(25, () -> {
            try (scene) {
                helper.assertTrue(scene.shots.size() == emittedBeforeCounter[0] && pair.core.isRemoved(),
                        "Counterspell must stop all remaining volleys");
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 60)
    public static void zeroCountAndMissedDeadlineCannotLeaveCoreActive(GameTestHelper helper) {
        var scene = new Scene(helper);
        var zero = scene.pair(new Vec3(2, 60, 2), new Vec3(0, 1, 0), 0);
        zero.core.acceptImpact(zero.arrow, zero.arrow.position().add(0, 10, 0));
        helper.assertTrue(zero.core.isRemoved(), "Zero follow-up count must finish immediately at impact");
        // worldへ登録しない核でtick停止を再現し、遅れて再開しても発射しないことを確認する。
        var arrow = new EchoArrowEntity(EntityRegistry.ECHO_ARROW.get(), helper.getLevel());
        var core = new EchoArrowCoreEntity(EntityRegistry.ECHO_ARROW_CORE.get(), helper.getLevel());
        core.configure(scene.owner, scene.pos(new Vec3(2, 60, 2)), 3, 3, arrow);
        core.acceptImpact(arrow, scene.pos(new Vec3(2, 70, 2)));
        helper.runAfterDelay(12, () -> {
            try (scene) {
                core.tick();
                helper.assertTrue(core.isRemoved() && scene.shots.size() == 1,
                        "Expired echoes must not resume or catch up missed volleys");
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 60)
    public static void blockedCoreConsumesArrowsWithoutSpawningAcrossWall(GameTestHelper helper) {
        var scene = new Scene(helper);
        scene.zombie(new Vec3(3, 30, 3));
        var pair = scene.pair(new Vec3(.98, 31, 3), new Vec3(1, 0, 0), 5);
        step(pair.arrow);
        var wall = new BlockPos(0, 31, 3);
        helper.setBlock(wall, Blocks.STONE);
        helper.runAfterDelay(17, () -> {
            try (scene) {
                helper.assertTrue(pair.core.isRemoved() && scene.shots.size() == 1,
                        "A blocked core must consume all scheduled slots without spawning arrows beyond the wall");
            } finally {
                helper.setBlock(wall, Blocks.AIR);
            }
            helper.succeed();
        });
    }

    private static void step(Entity entity) {
        entity.tickCount++;
        entity.tick();
    }

    private record Pair(EchoArrowEntity arrow, EchoArrowCoreEntity core) {
    }

    private record Shot(EchoArrowEntity arrow, long time, Vec3 origin, Vec3 velocity) {
    }

    private static final class Scene implements AutoCloseable {
        private final GameTestHelper helper;
        private final FakePlayer owner;
        private final List<Entity> entities = new ArrayList<>();
        private final List<Shot> shots = new ArrayList<>();
        private final Consumer<EntityJoinLevelEvent> listener;

        Scene(GameTestHelper helper) {
            this.helper = helper;
            owner = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "echo_test"));
            owner.setPos(pos(new Vec3(1, 30, 1)));
            owner.setNoGravity(true);
            add(owner);
            listener = event -> {
                if (event.getEntity() instanceof EchoArrowEntity arrow && arrow.getOwner() == owner) {
                    shots.add(new Shot(arrow, helper.getLevel().getGameTime(), arrow.position(), arrow.getDeltaMovement()));
                    if (!entities.contains(arrow)) entities.add(arrow);
                }
            };
            NeoForge.EVENT_BUS.addListener(listener);
        }

        Vec3 pos(Vec3 local) {
            return helper.absoluteVec(local);
        }

        void add(Entity entity) {
            if (!entities.contains(entity)) entities.add(entity);
            helper.getLevel().addFreshEntity(entity);
        }

        Zombie zombie(Vec3 local) {
            var zombie = EntityType.ZOMBIE.create(helper.getLevel());
            zombie.setPos(pos(local));
            zombie.setNoAi(true);
            zombie.setNoGravity(true);
            zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            zombie.setHealth(100);
            zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);
            add(zombie);
            return zombie;
        }

        EchoArrowEntity arrow(Vec3 origin, Vec3 direction, boolean initial, EchoArrowCoreEntity core) {
            var arrow = new EchoArrowEntity(EntityRegistry.ECHO_ARROW.get(), helper.getLevel());
            arrow.launch(owner, origin, direction, 3, initial, 2.5, core);
            add(arrow);
            return arrow;
        }

        Pair pair(Vec3 origin, Vec3 direction, int count) {
            var arrow = new EchoArrowEntity(EntityRegistry.ECHO_ARROW.get(), helper.getLevel());
            var core = new EchoArrowCoreEntity(EntityRegistry.ECHO_ARROW_CORE.get(), helper.getLevel());
            core.configure(owner, pos(origin), 3, count, arrow);
            arrow.launch(owner, pos(origin), direction, 3, true, 2.5, core);
            add(core);
            add(arrow);
            return new Pair(arrow, core);
        }

        @Override
        public void close() {
            NeoForge.EVENT_BUS.unregister(listener);
            List.copyOf(entities).forEach(Entity::discard);
        }
    }
}
