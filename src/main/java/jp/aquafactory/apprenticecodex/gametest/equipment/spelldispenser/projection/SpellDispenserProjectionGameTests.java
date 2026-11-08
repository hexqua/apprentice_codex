package jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.projection;

import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.spelldispenser.*;
import jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.DispenserTestScope;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.arcanebeam.ArcaneBeamEntity;
import jp.aquafactory.apprenticecodex.spell.fujin.FujinKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.fujin.FujinSlashProjectileEntity;
import jp.aquafactory.apprenticecodex.spell.shock.ShockBoltEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellDispenserValidation;
import static jp.aquafactory.apprenticecodex.gametest.equipment.spelldispenser.support.SpellDispenserTestSupport.createSpellScroll;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SpellDispenserProjectionGameTests {
    private SpellDispenserProjectionGameTests() {
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserCastFrameProjectionMovesAndRotatesMagicMissile(GameTestHelper helper) {
        try (var scope = new DispenserTestScope(helper)) {
            var level = (ServerLevel) helper.getLevel();
            var castBasePosition = helper.absoluteVec(new Vec3(1.5D, 2.5D, 1.5D));
            var projectedOrigin = helper.absoluteVec(new Vec3(4.5D, 3.0D, 4.5D));
            var projectedForward = new Vec3(1.0D, 0.0D, 0.0D);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            try (var ignored = SpellDispenserCastFrameBridge.useProjectorForGameTest((serverLevel, sourcePosition, frame) ->
                    new SpellDispenserCastFrameBridge.CastFrame(projectedOrigin, projectedForward))) {
                var result = scope.capture(() -> SpellDispenserCastHelper.tryCast(
                        level,
                        castBasePosition,
                        new Vec3(0.0D, 0.0D, 1.0D),
                        createSpellScroll(spell),
                        null
                ));
                helper.assertTrue(result.succeeded(), "Projected Spell Dispenser Magic Missile cast failed");
            }

            var spawnedProjectiles = scope.entities(MagicMissileProjectile.class);

            helper.assertTrue(!spawnedProjectiles.isEmpty(), "Projected Spell Dispenser cast did not spawn Magic Missile");
            var projectile = spawnedProjectiles.get(0);
            helper.assertTrue(Math.abs(projectile.getX() - projectedOrigin.x) < 0.01D
                            && Math.abs(projectile.getZ() - projectedOrigin.z) < 0.01D,
                    "Magic Missile did not spawn at the projected cast origin: " + projectile.position());
            var movement = projectile.getDeltaMovement().normalize();
            helper.assertTrue(movement.dot(projectedForward) > 0.999D,
                    "Magic Missile did not use the projected forward direction: " + projectile.getDeltaMovement());
            helper.assertTrue(Math.abs(projectile.getDeltaMovement().y) < 1.0E-6D
                            && Math.abs(projectile.getDeltaMovement().z) < 1.0E-6D,
                    "Magic Missile received an unexpected velocity component: " + projectile.getDeltaMovement());
            helper.succeed();
        }
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserCastFrameProjectionKeepsShockEndpointsInWorldFrame(GameTestHelper helper) {
        try (var scope = new DispenserTestScope(helper)) {
            var level = (ServerLevel) helper.getLevel();
            var castBasePosition = helper.absoluteVec(new Vec3(1.5D, 2.5D, 1.5D));
            var projectedOrigin = helper.absoluteVec(new Vec3(3.5D, 2.5D, 2.5D));
            var projectedForward = new Vec3(0.0D, 0.0D, 1.0D);
            var targetPos = new BlockPos(3, 2, 6);
            helper.setBlock(targetPos, Blocks.GOLD_BLOCK);

            try (var ignored = SpellDispenserCastFrameBridge.useProjectorForGameTest((serverLevel, sourcePosition, frame) ->
                    new SpellDispenserCastFrameBridge.CastFrame(projectedOrigin, projectedForward))) {
                var result = scope.capture(() -> SpellDispenserCastHelper.tryCast(
                        level,
                        castBasePosition,
                        new Vec3(1.0D, 0.0D, 0.0D),
                        createSpellScroll(SpellRegistry.SHOCK.get()),
                        null
                ));
                helper.assertTrue(result.succeeded(), "Projected Spell Dispenser Shock cast failed");
            }

            var bolts = scope.entities(ShockBoltEntity.class);
            helper.assertTrue(!bolts.isEmpty(), "Projected Spell Dispenser cast did not spawn Shock Bolt");
            var bolt = bolts.get(0);
            var expectedStart = projectedOrigin.add(0.0D, -0.22D, 0.0D).add(projectedForward.scale(0.55D));
            assertVecClose(helper, expectedStart, bolt.position(), 0.01D,
                    "Shock Bolt start was not projected into the world frame");

            var end = bolt.getEndPosition();
            var targetCenter = Vec3.atCenterOf(helper.absolutePos(targetPos));
            // Shock Bolt の同期終点は float のため、GameTest が配置される大座標では1ブロック程度丸められる。
            helper.assertTrue(end.distanceToSqr(targetCenter) <= 4.0D,
                    "Shock Bolt did not stop near the projected target block: end=" + end + ", target=" + targetCenter);
            helper.assertTrue(end.subtract(bolt.position()).normalize().dot(projectedForward) > 0.8D,
                    "Shock Bolt end remained in a different coordinate frame: " + end);
            helper.succeed();
        }
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserCastFrameProjectionAppliesProfileOffsetsBeforeProjection(GameTestHelper helper) {
        try (var scope = new DispenserTestScope(helper)) {
            var level = (ServerLevel) helper.getLevel();
            var castBasePosition = helper.absoluteVec(new Vec3(2.5D, 2.5D, 2.5D));
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.MAGIC_MISSILE_SPELL.get();
            var profile = new SpellDispenserSpellProfile(
                    SpellDispenserCastAnchorMode.AUTO,
                    SpellDispenserCasterMode.NEUTRAL_LIVING,
                    0.3D,
                    0.5D,
                    0.25D,
                    90.0F,
                    0.0F,
                    false
            );
            var capturedFrame = new AtomicReference<SpellDispenserCastFrameBridge.CastFrame>();

            try (var ignoredProfiles = SpellDispenserSpellProfileManager.useProfilesForGameTest(Map.of(spell.getSpellResource(), profile));
                 var ignoredProjector = SpellDispenserCastFrameBridge.useProjectorForGameTest((serverLevel, sourcePosition, frame) -> {
                     capturedFrame.set(frame);
                     return frame;
                 })) {
                var result = scope.capture(() -> SpellDispenserCastHelper.tryCast(
                        level,
                        castBasePosition,
                        new Vec3(0.0D, 0.0D, 1.0D),
                        createSpellScroll(spell),
                        null
                ));
                helper.assertTrue(result.succeeded(), "Offset Spell Dispenser Magic Missile cast failed");
            }

            var frame = capturedFrame.get();
            helper.assertTrue(frame != null, "Spell Dispenser cast frame projector was not invoked");
            var expectedOrigin = castBasePosition.add(-0.25D, 0.5D, 1.0D);
            assertVecClose(helper, expectedOrigin, frame.origin(), 1.0E-6D,
                    "Spell profile position offsets were not applied before projection");
            var expectedForward = Vec3.directionFromRotation(0.0F, 90.0F).normalize();
            helper.assertTrue(frame.forward().normalize().dot(expectedForward) > 0.999D,
                    "Spell profile angle offsets were not applied before projection: " + frame.forward());
            helper.succeed();
        }
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserContinuousProjectionUpdatesFireBreathAnchor(GameTestHelper helper) {
        try (var scope = new DispenserTestScope(helper)) {
            var level = (ServerLevel) helper.getLevel();
            var castBasePosition = helper.absoluteVec(new Vec3(1.5D, 2.5D, 1.5D));
            var projectedOrigin = helper.absoluteVec(new Vec3(4.5D, 3.0D, 4.5D));
            var projectedForward = new Vec3(1.0D, 0.0D, 0.0D);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIRE_BREATH_SPELL.get();
            var scrollStack = createSpellScroll(spell);

            try (var ignored = SpellDispenserCastFrameBridge.useProjectorForGameTest((serverLevel, sourcePosition, frame) ->
                    new SpellDispenserCastFrameBridge.CastFrame(projectedOrigin, projectedForward))) {
                var startResult = scope.start(() -> SpellDispenserCastHelper.tryStartContinuousCast(
                        level,
                        castBasePosition,
                        new Vec3(0.0D, 0.0D, 1.0D),
                        SpellDispenserSpellValidator.validate(scrollStack),
                        scrollStack,
                        null
                ));
                helper.assertTrue(startResult.result().succeeded() && startResult.session() != null,
                        "Projected Spell Dispenser Fire Breath did not start");

                var session = startResult.session();
                scope.run(() -> SpellDispenserCastHelper.syncContinuousCastTransform(
                        level,
                        session,
                        castBasePosition,
                        new Vec3(0.0D, 0.0D, 1.0D)
                ));
                scope.run(() -> SpellDispenserCastHelper.tickContinuousCast(level, session));

                var anchor = session.trackedAnchor();
                helper.assertTrue(anchor != null, "Projected Fire Breath did not create its tracked anchor");
                helper.assertTrue(anchor.getLookAngle().normalize().dot(projectedForward) > 0.999D,
                        "Projected Fire Breath anchor did not use the projected direction: " + anchor.getLookAngle());
                helper.assertTrue(anchor.getEyePosition().distanceToSqr(session.proxy().getEyePosition()) < 1.0E-6D,
                        "Projected Fire Breath anchor did not follow the proxy caster");

                var projectiles = scope.entities(FireBreathProjectile.class);
                helper.assertTrue(!projectiles.isEmpty(), "Projected Fire Breath did not spawn in the world frame");
                helper.assertTrue(projectiles.get(0).getOwner() == anchor,
                        "Projected Fire Breath did not retain its tracked anchor owner");

                scope.run(() -> SpellDispenserCastHelper.finishContinuousCast(level, session, true));
                helper.assertTrue(anchor.isRemoved(), "Projected Fire Breath anchor remained after cleanup");
            }
            helper.succeed();
        }
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserContinuousProjectionUpdatesBlazeStormShots(GameTestHelper helper) {
        try (var scope = new DispenserTestScope(helper)) {
            var level = (ServerLevel) helper.getLevel();
            var castBasePosition = helper.absoluteVec(new Vec3(1.5D, 2.5D, 1.5D));
            var projectedOrigin = helper.absoluteVec(new Vec3(4.5D, 3.0D, 4.5D));
            var projectedForward = new Vec3(-1.0D, 0.0D, 0.0D);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
            );
            var scrollStack = createSpellScroll(spell);

            try (var ignored = SpellDispenserCastFrameBridge.useProjectorForGameTest((serverLevel, sourcePosition, frame) ->
                    new SpellDispenserCastFrameBridge.CastFrame(projectedOrigin, projectedForward))) {
                var startResult = scope.start(() -> SpellDispenserCastHelper.tryStartContinuousCast(
                        level,
                        castBasePosition,
                        new Vec3(0.0D, 0.0D, 1.0D),
                        createSpellDispenserValidation(scrollStack, spell),
                        scrollStack,
                        null
                ));
                helper.assertTrue(startResult.result().succeeded() && startResult.session() != null,
                        "Projected Spell Dispenser Blaze Storm did not start");

                var session = startResult.session();
                scope.run(() -> SpellDispenserCastHelper.syncContinuousCastTransform(
                        level,
                        session,
                        castBasePosition,
                        new Vec3(0.0D, 0.0D, 1.0D)
                ));
                scope.run(() -> SpellDispenserCastHelper.tickContinuousCast(level, session));

                var fireballs = scope.entities(SmallMagicFireball.class);
                helper.assertTrue(!fireballs.isEmpty(), "Projected Blaze Storm did not spawn a fireball");
                helper.assertTrue(fireballs.get(0).getDeltaMovement().normalize().dot(projectedForward) > 0.995D,
                        "Projected Blaze Storm fireball used the wrong direction: " + fireballs.get(0).getDeltaMovement());

                scope.run(() -> SpellDispenserCastHelper.finishContinuousCast(level, session, true));
            }
            helper.succeed();
        }
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserContinuousProjectionUpdatesArcaneBeam(GameTestHelper helper) {
        try (var scope = new DispenserTestScope(helper)) {
            var level = (ServerLevel) helper.getLevel();
            var castBasePosition = helper.absoluteVec(new Vec3(1.5D, 2.5D, 1.5D));
            var projectedFrame = new AtomicReference<>(new SpellDispenserCastFrameBridge.CastFrame(
                    helper.absoluteVec(new Vec3(3.5D, 3.0D, 3.5D)),
                    new Vec3(1.0D, 0.0D, 0.0D)
            ));
            var spell = SpellRegistry.ARCANE_BEAM.get();
            var scrollStack = createSpellScroll(spell);

            try (var ignored = SpellDispenserCastFrameBridge.useProjectorForGameTest((serverLevel, sourcePosition, frame) ->
                    projectedFrame.get())) {
                var startResult = scope.start(() -> SpellDispenserCastHelper.tryStartContinuousCast(
                        level,
                        castBasePosition,
                        new Vec3(0.0D, 0.0D, 1.0D),
                        SpellDispenserSpellValidator.validate(scrollStack),
                        scrollStack,
                        null
                ));
                helper.assertTrue(startResult.result().succeeded() && startResult.session() != null,
                        "Projected Spell Dispenser Arcane Beam did not start");

                var session = startResult.session();
                scope.run(() -> SpellDispenserCastHelper.tickContinuousCast(level, session));
                var beams = scope.entities(ArcaneBeamEntity.class);
                helper.assertTrue(beams.size() == 1, "Projected Arcane Beam did not create exactly one beam");

                projectedFrame.set(new SpellDispenserCastFrameBridge.CastFrame(
                        helper.absoluteVec(new Vec3(5.5D, 3.5D, 5.5D)),
                        new Vec3(0.0D, 0.0D, -1.0D)
                ));
                scope.run(() -> SpellDispenserCastHelper.syncContinuousCastTransform(
                        level,
                        session,
                        castBasePosition,
                        new Vec3(0.0D, 0.0D, 1.0D)
                ));
                scope.run(() -> SpellDispenserCastHelper.tickContinuousCast(level, session));

                var beam = beams.get(0);
                var expectedPosition = session.proxy().getEyePosition().add(0.0D, -0.7D, 0.0D)
                        .add(projectedFrame.get().forward().scale(0.75D));
                assertVecClose(helper, expectedPosition, beam.position(), 0.01D,
                        "Arcane Beam did not follow the updated projected origin");
                helper.assertTrue(beam.getLookAngle().normalize().dot(projectedFrame.get().forward()) > 0.999D,
                        "Arcane Beam did not follow the updated projected direction: " + beam.getLookAngle());

                scope.run(() -> SpellDispenserCastHelper.finishContinuousCast(level, session, true));
                helper.assertTrue(beam.isRemoved(), "Projected Arcane Beam remained after cleanup");
            }
            helper.succeed();
        }
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserContinuousProjectionUpdatesFujinWeapon(GameTestHelper helper) {
        try (var scope = new DispenserTestScope(helper)) {
            var level = (ServerLevel) helper.getLevel();
            var castBasePosition = helper.absoluteVec(new Vec3(1.5D, 2.5D, 1.5D));
            var projectedFrame = new AtomicReference<>(new SpellDispenserCastFrameBridge.CastFrame(
                    helper.absoluteVec(new Vec3(3.5D, 3.0D, 3.5D)),
                    new Vec3(1.0D, 0.0D, 0.0D)
            ));
            var spell = SpellRegistry.FUJIN.get();
            var scrollStack = createSpellScroll(spell);

            try (var ignored = SpellDispenserCastFrameBridge.useProjectorForGameTest((serverLevel, sourcePosition, frame) ->
                    projectedFrame.get())) {
                var startResult = scope.start(() -> SpellDispenserCastHelper.tryStartContinuousCast(
                        level,
                        castBasePosition,
                        new Vec3(0.0D, 0.0D, 1.0D),
                        SpellDispenserSpellValidator.validate(scrollStack),
                        scrollStack,
                        null
                ));
                helper.assertTrue(startResult.result().succeeded() && startResult.session() != null,
                        "Projected Spell Dispenser Fujin did not start");

                var session = startResult.session();
                for (var tick = 0; tick < 10 && !session.hasReachedOnCast(); tick++) {
                    scope.run(() -> SpellDispenserCastHelper.tickContinuousCast(level, session));
                }
                helper.assertTrue(session.hasReachedOnCast(),
                        "Projected Fujin did not reach its first CONTINUOUS onCast interval");
                var weapons = scope.entities(FujinKatanaEntity.class);
                helper.assertTrue(weapons.size() == 1,
                        "Projected Fujin did not create exactly one katana: " + weapons.size());
                var weapon = weapons.get(0);
                helper.assertTrue(weapon.getOwner() == session.proxy(),
                        "Projected Fujin katana did not retain the proxy caster as owner");

                projectedFrame.set(new SpellDispenserCastFrameBridge.CastFrame(
                        helper.absoluteVec(new Vec3(5.5D, 3.5D, 5.5D)),
                        new Vec3(0.0D, 0.0D, -1.0D)
                ));
                scope.run(() -> SpellDispenserCastHelper.syncContinuousCastTransform(
                        level,
                        session,
                        castBasePosition,
                        new Vec3(0.0D, 0.0D, 1.0D)
                ));
                // 発射周期はFujin固有テストへ任せ、投影後の向きによる発射だけを観測する。
                weapon.tickCount = 10;
                scope.run(() -> weapon.tickOnServer(level));
                var slashes = scope.entities(FujinSlashProjectileEntity.class);

                helper.assertTrue(weapon.position().distanceToSqr(session.proxy().position()) < 4.0D,
                        "Projected Fujin katana did not follow the updated proxy position: " + weapon.position());
                helper.assertTrue(!slashes.isEmpty(), "Projected Fujin katana did not fire a slash");
                helper.assertTrue(slashes.get(0).getDeltaMovement().normalize().dot(projectedFrame.get().forward()) > 0.999D,
                        "Projected Fujin slash used the wrong direction: " + slashes.get(0).getDeltaMovement());

                scope.run(() -> SpellDispenserCastHelper.finishContinuousCast(level, session, true));
                helper.assertTrue(weapon.isRemoved(), "Projected Fujin katana remained after cleanup");
            }
            helper.succeed();
        }
    }

    @GameTest(template = "gametest/dispenser_casting")
    public static void spellDispenserBlockEntityReprojectsContinuousCastEachTick(GameTestHelper helper) {
        try (var scope = new DispenserTestScope(helper)) {
            var level = (ServerLevel) helper.getLevel();
            var pos = new BlockPos(0, 1, 0);
            helper.setBlock(pos, BlockRegistry.SPELL_DISPENSER.get());
            helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, true));

            var blockEntity = helper.getBlockEntity(pos);
            helper.assertTrue(blockEntity instanceof SpellDispenserBlockEntity,
                    "Spell Dispenser block entity was not created for projected CONTINUOUS tick test");
            var spellDispenser = scope.block((SpellDispenserBlockEntity) blockEntity);
            var spell = io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "blaze_storm")
            );
            var scrollStack = createSpellScroll(spell);
            spellDispenser.getInventory().setStackInSlot(0, scrollStack);

            var projectedFrame = new AtomicReference<>(new SpellDispenserCastFrameBridge.CastFrame(
                    helper.absoluteVec(new Vec3(2.5D, 2.5D, 2.5D)),
                    new Vec3(1.0D, 0.0D, 0.0D)
            ));
            var projectionCount = new AtomicInteger();
            try (var ignored = SpellDispenserCastFrameBridge.useProjectorForGameTest((serverLevel, sourcePosition, frame) -> {
                projectionCount.incrementAndGet();
                return projectedFrame.get();
            })) {
                var startResult = scope.start(() -> SpellDispenserCastHelper.tryStartContinuousCast(
                        level,
                        helper.absolutePos(pos),
                        Direction.NORTH,
                        createSpellDispenserValidation(scrollStack, spell),
                        scrollStack,
                        null
                ));
                helper.assertTrue(startResult.result().succeeded() && startResult.session() != null,
                        "Spell Dispenser did not start projected BlockEntity CONTINUOUS test session");
                var session = startResult.session();
                spellDispenser.startContinuousCast(session);

                projectedFrame.set(new SpellDispenserCastFrameBridge.CastFrame(
                        helper.absoluteVec(new Vec3(5.5D, 3.5D, 5.5D)),
                        new Vec3(0.0D, 0.0D, -1.0D)
                ));
                scope.run(() -> SpellDispenserBlockEntity.serverTick(level, helper.absolutePos(pos), helper.getBlockState(pos), spellDispenser));

                helper.assertTrue(projectionCount.get() >= 2,
                        "Spell Dispenser BlockEntity tick did not re-run CONTINUOUS cast projection");
                assertVecClose(helper, projectedFrame.get().origin(), session.proxy().getEyePosition(), 0.01D,
                        "Spell Dispenser BlockEntity tick did not move the CONTINUOUS proxy to the projected origin");
                helper.assertTrue(session.proxy().getLookAngle().normalize().dot(projectedFrame.get().forward()) > 0.999D,
                        "Spell Dispenser BlockEntity tick did not rotate the CONTINUOUS proxy to the projected direction");

                helper.setBlock(pos, helper.getBlockState(pos).setValue(SpellDispenser.TRIGGERED, false));
                scope.run(() -> SpellDispenserBlockEntity.serverTick(level, helper.absolutePos(pos), helper.getBlockState(pos), spellDispenser));
                helper.assertFalse(spellDispenser.hasActiveContinuousCast(),
                        "Projected BlockEntity CONTINUOUS test session remained after signal loss");
            }
            helper.succeed();
        }
    }

    private static void assertVecClose(
            GameTestHelper helper,
            Vec3 expected,
            Vec3 actual,
            double tolerance,
            String message
    ) {
        helper.assertTrue(expected.distanceToSqr(actual) <= tolerance * tolerance,
                message + ": expected=" + expected + ", actual=" + actual);
    }

}
