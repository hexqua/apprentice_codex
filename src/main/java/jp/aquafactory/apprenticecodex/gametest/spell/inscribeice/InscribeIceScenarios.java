package jp.aquafactory.apprenticecodex.gametest.spell.inscribeice;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import jp.aquafactory.apprenticecodex.registry.EffectRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIce;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceBurst;
import jp.aquafactory.apprenticecodex.spell.inscribeice.InscribeIceDaggerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import static jp.aquafactory.apprenticecodex.gametest.support.DamageTestSupport.setMaxHealthForDamageTest;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;

final class InscribeIceScenarios {
    private InscribeIceScenarios() {}

    static void inscribeIceCastUsesShortThrowJob(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "inscribe_ice_fan_test");
        player.setYRot(0.0F);
        player.setXRot(-30.0F);

        var spell = (InscribeIce) SpellRegistry.INSCRIBE_ICE.get();
        spell.onCast(level, 5, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));

        var expectedCount = spell.getProjectileCount(5, player);
        var firstWave = level.getEntitiesOfClass(
                InscribeIceDaggerEntity.class,
                player.getBoundingBox().inflate(4.0D),
                projectile -> projectile.getOwner() == player
        );
        helper.assertTrue(firstWave.size() > 0 && firstWave.size() < expectedCount,
                "Inscribe Ice should spawn the first dagger wave immediately without creating all daggers: "
                        + firstWave.size() + " / " + expectedCount);

        var expectedSpawnPosition = player.getEyePosition()
                .add(player.getLookAngle().normalize().scale(0.4D))
                .add(0.0D, -0.25D, 0.0D);
        for (var projectile : firstWave) {
            assertInscribeIceDaggerLaunch(helper, projectile);
            helper.assertTrue(projectile.position().distanceToSqr(expectedSpawnPosition) < 0.08D,
                    "Inscribe Ice dagger should spawn near the centered magic missile style launch position");
        }

        helper.runAfterDelay(3, () -> {
            var spawned = level.getEntitiesOfClass(
                    InscribeIceDaggerEntity.class,
                    player.getBoundingBox().inflate(16.0D),
                    projectile -> projectile.getOwner() == player
            );
            helper.assertTrue(spawned.size() == expectedCount,
                    "Inscribe Ice short throw job should finish within 3 ticks: "
                            + spawned.size() + " / " + expectedCount);

            var sorted = spawned.stream()
                    .sorted(Comparator.comparingDouble(projectile -> -projectile.getDeltaMovement().x))
                    .toList();
            for (var projectile : sorted) {
                assertInscribeIceDaggerLaunch(helper, projectile);
            }

            helper.assertTrue(sorted.get(0).getDeltaMovement().x > 0.01D,
                    "Inscribe Ice should include the caster's right side of the fan");
            helper.assertTrue(sorted.get(sorted.size() - 1).getDeltaMovement().x < -0.01D,
                    "Inscribe Ice should include the caster's left side of the fan");
            assertInscribeIceCenterDaggerLaunchesForward(helper, player, sorted);
            helper.succeed();
        });
    }

    static void assertInscribeIceCenterDaggerLaunchesForward(GameTestHelper helper, LivingEntity caster,
                                                             List<InscribeIceDaggerEntity> projectiles) {
        var centerProjectile = projectiles.stream()
                .min(Comparator.comparingDouble(projectile -> Math.abs(projectile.getDeltaMovement().x)))
                .orElseThrow();
        var movement = centerProjectile.getDeltaMovement();
        var expectedForward = caster.getLookAngle().normalize();
        var actualForward = movement.normalize();
        helper.assertTrue(Math.abs(movement.x) < 1.0E-6D,
                "Inscribe Ice center dagger should not receive yaw jitter: " + movement);
        helper.assertTrue(actualForward.dot(expectedForward) > 0.999D,
                "Inscribe Ice center dagger should follow the caster's exact look direction: " + movement);
    }

    static void assertInscribeIceDaggerLaunch(GameTestHelper helper, InscribeIceDaggerEntity projectile) {
        helper.assertTrue(projectile.isNoGravity(), "Inscribe Ice dagger should not use gravity");
        helper.assertTrue(projectile.getDeltaMovement().y > 0.1D,
                "Inscribe Ice dagger should follow the caster's upward look direction: "
                        + projectile.getDeltaMovement());

        var speed = projectile.getDeltaMovement().length();
        helper.assertTrue(speed >= InscribeIceDaggerEntity.SPEED * 0.91D
                        && speed <= InscribeIceDaggerEntity.SPEED * 1.09D,
                "Inscribe Ice dagger speed should stay near 70% of Sky Edge baseline: " + speed);
    }

    static void notchedFrozenStacksAndBurstsOnThirdStack(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var level = helper.getLevel();
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "notched_frozen_owner_test");
            var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            setMaxHealthForDamageTest(target, 100.0F);

            InscribeIceDaggerEntity.applyNotchedFrozenOrBurst(level, target, owner, owner, 4.0F);
            assertNotchedFrozen(helper, target, 0);
            InscribeIceDaggerEntity.applyNotchedFrozenOrBurst(level, target, owner, owner, 4.0F);
            assertNotchedFrozen(helper, target, 1);
            InscribeIceDaggerEntity.applyNotchedFrozenOrBurst(level, target, owner, owner, 4.0F);

            helper.assertFalse(target.hasEffect(EffectRegistry.NOTCHED_FROZEN),
                    "Inscribe Ice should remove Notched Frozen when the third application succeeds");
            helper.assertTrue(target.getHealth() < 100.0F,
                    "Inscribe Ice burst should damage the detonated target");
        });
    }

    static void notchedFrozenMaintainsExistingFreezeWithoutIceWeakness(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var target = helper.spawn(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            var effect = EffectRegistry.NOTCHED_FROZEN.get();
            var iceResistance = target.getAttribute(AttributeRegistry.ICE_MAGIC_RESIST);
            helper.assertTrue(iceResistance != null, "Notched Frozen test could not resolve ice magic resistance attribute");
            var baseIceResistance = iceResistance == null ? 0.0D : iceResistance.getValue();

            target.addEffect(new MobEffectInstance(EffectRegistry.NOTCHED_FROZEN, 20, 0, false, false, true));
            var currentIceResistance = iceResistance == null ? 0.0D : iceResistance.getValue();
            helper.assertTrue(currentIceResistance == baseIceResistance,
                    "Notched Frozen should not modify ice spell resistance while disabled");
            target.removeEffect(EffectRegistry.NOTCHED_FROZEN);

            target.setTicksFrozen(40);
            effect.applyEffectTick(target, 0);
            helper.assertTrue(target.getTicksFrozen() == 42,
                    "Notched Frozen should offset natural frozen tick decay when freezing is already active");

            target.setTicksFrozen(0);
            effect.applyEffectTick(target, 0);
            helper.assertTrue(target.getTicksFrozen() == 0,
                    "Notched Frozen should not start freezing by itself");
        });
    }

    static void inscribeIceBurstUsesHalfDamageForChainedBurstsAndSkipsPlayers(GameTestHelper helper) {
        helper.runAfterDelay(3, () -> {
            var level = helper.getLevel();
            var owner = createEquipmentTestPlayer(helper, new BlockPos(0, 2, 0), "inscribe_ice_burst_owner_test");
            var directOrigin = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            var directTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 0));
            var chainOrigin = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, 2, 0));
            var chainTarget = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, 2, 0));
            var playerTarget = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 1), "inscribe_ice_burst_player_test");

            setMaxHealthForDamageTest(directOrigin, 100.0F);
            setMaxHealthForDamageTest(directTarget, 100.0F);
            setMaxHealthForDamageTest(chainOrigin, 100.0F);
            setMaxHealthForDamageTest(chainTarget, 100.0F);
            playerTarget.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0D);
            playerTarget.setHealth(100.0F);

            InscribeIceBurst.burstFromDagger(level, directOrigin, owner, owner, 10.0F);
            InscribeIceBurst.burstChain(level, chainOrigin, owner, owner, 5.0F, new HashSet<>());

            var directLoss = 100.0F - directTarget.getHealth();
            var chainLoss = 100.0F - chainTarget.getHealth();
            helper.assertTrue(directLoss > 0.0F && chainLoss > 0.0F,
                    "Inscribe Ice direct and chained bursts should both damage nearby mobs");
            helper.assertTrue(Math.abs(chainLoss * 2.0F - directLoss) < 0.25F,
                    "Inscribe Ice chained burst damage should be half of direct burst damage: direct="
                            + directLoss + ", chain=" + chainLoss);
            helper.assertTrue(playerTarget.getHealth() == 100.0F,
                    "Inscribe Ice burst should not damage players in the blast area");
            helper.succeed();
        });
    }

    static void assertNotchedFrozen(GameTestHelper helper, LivingEntity target, int expectedAmplifier) {
        var instance = target.getEffect(EffectRegistry.NOTCHED_FROZEN);
        helper.assertTrue(instance != null, "Target should have Notched Frozen");
        helper.assertTrue(instance != null && instance.getAmplifier() == expectedAmplifier,
                "Notched Frozen amplifier mismatch: expected=" + expectedAmplifier
                        + ", actual=" + (instance == null ? "missing" : instance.getAmplifier()));
        helper.assertTrue(instance != null && instance.getDuration() == 20 * 15,
                "Notched Frozen should refresh to 15 seconds");
        helper.assertTrue(instance != null && !instance.isVisible(),
                "Notched Frozen should suppress vanilla potion particles");
    }
}
