package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.echo;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.echoarrow.EchoArrowCoreEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.echo.EchoArrowFixture.step;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoArrowFlightGameTests {
    private EchoArrowFlightGameTests() {
    }

    private static final String TEMPLATE = "gametest/echo_arrow_flight";
    private static final String BATCH = "apprenticecodex.echo_arrow";

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void initialFlightMatchesBloodyArrowAndExpiresWithCore(GameTestHelper helper) {
        try (var scene = new EchoArrowFixture(helper)) {
            var pair = scene.pair(new Vec3(2, 40, 2), new Vec3(0, 1, 0), 7);
            Vec3 origin = pair.arrow().position();
            for (int i = 0; i < 10; i++) step(pair.arrow());
            helper.assertTrue(pair.arrow().position().distanceTo(origin.add(0, 25, 0)) < 1e-6,
                    "Initial arrow must travel ten ticks at 2.5 blocks per tick");
            step(pair.arrow());
            helper.assertTrue(Math.abs(pair.arrow().getDeltaMovement().y - 2.45) < 1e-6,
                    "Gravity must start after the eleventh movement, matching BloodyArrow");
            pair.arrow().setPos(origin);
            pair.arrow().setDeltaMovement(Vec3.ZERO);
            pair.arrow().setNoGravity(true);
            while (pair.arrow().tickCount < 199) step(pair.arrow());
            helper.assertFalse(pair.arrow().isRemoved(), "Initial arrow must survive until tick 200");
            step(pair.arrow());
            helper.assertTrue(pair.arrow().isRemoved() && pair.core().isRemoved(), "Expiration must also remove the waiting core");
            helper.assertFalse(pair.arrow().shouldBeSaved() || pair.core().shouldBeSaved(), "Echo entities must never persist");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void bothModesIgnoreArmorIframesAndKnockbackWithoutPiercing(GameTestHelper helper) {
        try (var scene = new EchoArrowFixture(helper)) {
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
        try (var scene = new EchoArrowFixture(helper)) {
            var pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            pair.arrow().onAntiMagic(null);
            helper.assertTrue(pair.core().isRemoved(), "Countering the initial arrow must remove its core");
            pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            pair.core().onAntiMagic(null);
            step(pair.arrow());
            helper.assertFalse(pair.arrow().isRemoved(), "Countering the core must not remove its initial arrow");
            pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            pair.arrow().remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            helper.assertTrue(pair.core().isRemoved(), "Unloading the initial arrow must remove the waiting core");
            pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            pair.core().remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            helper.assertFalse(pair.arrow().isRemoved(), "Unloading a core must not destroy an already fired arrow");
            var followup = scene.arrow(scene.pos(new Vec3(2, 30, 2)), new Vec3(0, 1, 0), false, null);
            followup.onAntiMagic(null);
            helper.assertTrue(followup.isRemoved(), "Follow-up arrows must also support anti-magic");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void realCounterspellRayCanSelectCore(GameTestHelper helper) {
        try (var scene = new EchoArrowFixture(helper)) {
            var pair = scene.pair(new Vec3(3, 31.62, 3), new Vec3(0, 1, 0), 7);
            pair.arrow().setPos(pair.arrow().position().add(0, 5, 0));
            scene.owner.setPos(scene.pos(new Vec3(.5, 30, 3)));
            scene.owner.setYRot(-90);
            scene.owner.setXRot(0);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.COUNTERSPELL_SPELL.get();
            scene.scope.run(() -> spell.onCast(helper.getLevel(), 1, scene.owner, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(scene.owner)));
            helper.assertTrue(pair.core().isRemoved(), "The real Counterspell ray must select the visible core");
            helper.assertFalse(pair.arrow().isRemoved(), "Counterspell on the core must preserve the initial arrow");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void terrainStopsBothModesAndCancelledImpactDoesNotArm(GameTestHelper helper) {
        try (var scene = new EchoArrowFixture(helper)) {
            var wall = new BlockPos(3, 31, 3);
            scene.setBlock(wall, Blocks.STONE);
            try {
                for (boolean initial : new boolean[]{true, false}) {
                    var arrow = scene.arrow(scene.pos(new Vec3(.8, 31.5, 3.5)), new Vec3(1, 0, 0), initial, null);
                    step(arrow);
                    helper.assertTrue(arrow.isRemoved() && arrow.getX() <= scene.pos(new Vec3(3, 0, 0)).x + .001,
                            "Terrain must stop initial and follow-up arrows");
                }
            } finally {
                scene.setBlock(wall, Blocks.AIR);
            }
            var target = scene.zombie(new Vec3(3, 30, 3));
            var pair = scene.pair(new Vec3(.8, 31, 3), new Vec3(1, 0, 0), 7);
            Consumer<ProjectileImpactEvent> listener = event -> {
                if (event.getProjectile() == pair.arrow()) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                step(pair.arrow());
                helper.assertTrue(!pair.arrow().isRemoved() && pair.core().startTime() < 0 && target.getHealth() == 100,
                        "Cancelled impacts must not arm the core or deal damage");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void ownerLossAndUnloadedDestinationEndEntities(GameTestHelper helper) {
        try (var scene = new EchoArrowFixture(helper)) {
            var pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            scene.owner.setHealth(0);
            pair.core().tick();
            step(pair.arrow());
            helper.assertTrue(pair.core().isRemoved() && pair.arrow().isRemoved(), "Owner death must stop both entities");
        }
        try (var scene = new EchoArrowFixture(helper)) {
            var pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            scene.owner.remove(Entity.RemovalReason.CHANGED_DIMENSION);
            pair.core().tick();
            step(pair.arrow());
            helper.assertTrue(pair.core().isRemoved() && pair.arrow().isRemoved(), "Removed owners must not keep echoes active");
        }
        try (var scene = new EchoArrowFixture(helper)) {
            var pair = scene.pair(new Vec3(2, 30, 2), new Vec3(0, 1, 0), 7);
            // 専用structureで周辺chunkが広く読まれても、未読込という入力条件を先に保証する。
            Vec3 movement = new Vec3(1024, 0, 0);
            if (pair.arrow().getX() > 0) movement = movement.scale(-1);
            while (helper.getLevel().hasChunkAt(BlockPos.containing(pair.arrow().position().add(movement)))
                    && movement.lengthSqr() < 8192.0D * 8192.0D) movement = movement.scale(2);
            helper.assertFalse(helper.getLevel().hasChunkAt(BlockPos.containing(pair.arrow().position().add(movement))),
                    "The test destination must be unloaded before flight");
            pair.arrow().setDeltaMovement(movement);
            step(pair.arrow());
            helper.assertTrue(pair.arrow().isRemoved() && pair.core().isRemoved(), "Unloaded destinations must end the shot without force-loading");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void castUsesExistingValuesAndImpactDuration(GameTestHelper helper) {
        try (var scene = new EchoArrowFixture(helper)) {
            var spell = SpellRegistry.ECHO_ARROW.get();
            var info = spell.getUniqueInfo(1, scene.owner);
            var args = ((TranslatableContents) info.get(1).getContents()).getArgs();
            int count = Integer.parseInt(args[0].toString());
            float duration = Float.parseFloat(((TranslatableContents) info.get(2).getContents()).getArgs()[0].toString().replace("s", ""));
            helper.assertTrue(Math.abs(duration - (10 + count - 1) / 20f) < .011,
                    "Tooltip must describe time from impact to the last shot");
            scene.scope.run(() -> spell.onCast(helper.getLevel(), 1, scene.owner, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(scene.owner)));
            var origin = scene.owner.getEyePosition().add(scene.owner.getForward()).add(0, -0.4, 0);
            helper.assertTrue(scene.shots.size() == 1 && scene.shots.getFirst().origin().distanceTo(origin) < 1e-6,
                    "Casting must emit exactly one initial arrow at the fixed arrow offset");
            var cores = scene.scope.entities(EchoArrowCoreEntity.class);
            helper.assertTrue(cores.size() == 1 && cores.getFirst().totalCount() == count,
                    "The core must snapshot the advertised follow-up count");
            helper.assertTrue(cores.getFirst().position().distanceTo(origin) < 1e-6,
                    "The core must share the initial arrow's offset origin");
            cores.forEach(Entity::discard);
        }
        helper.succeed();
    }
}
