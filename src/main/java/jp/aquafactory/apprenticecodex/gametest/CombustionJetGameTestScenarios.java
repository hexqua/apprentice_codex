package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.combustionjet.CombustionJet;
import jp.aquafactory.apprenticecodex.spell.combustionjet.CombustionJetWaveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.UUID;

final class CombustionJetGameTestScenarios {
    private static final double PROJECTILE_TEST_HEIGHT = 20.0D;
    private static final double POSITION_EPSILON = 1.0E-5D;
    private static final float VALUE_EPSILON = 1.0E-4F;

    private CombustionJetGameTestScenarios() {
    }

    static void combustionJetCastSpawnsConfiguredWave(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "combustion_jet_cast");
        owner.setYRot(0.0F);
        owner.setXRot(0.0F);
        var spell = (CombustionJet) SpellRegistry.COMBUSTION_JET.get();

        spell.onCast(level, 1, owner, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(owner));

        var searchBounds = owner.getBoundingBox().inflate(4.0D);
        var waves = level.getEntitiesOfClass(CombustionJetWaveEntity.class, searchBounds);
        helper.assertTrue(waves.size() == 1, "Combustion Jet should spawn exactly one wave");
        var wave = waves.getFirst();
        helper.assertTrue(Math.abs(wave.getDeltaMovement().length() - 2.25D) < POSITION_EPSILON,
                "Combustion Jet wave should be 2.25d speed");
        helper.assertTrue(Math.abs(wave.getMaxTravelDistanceForGameTest() - 10.0F) < VALUE_EPSILON,
                "Combustion Jet wave should use the spell range");
        helper.assertTrue(wave.getDamageForGameTest() > 0.0F,
                "Combustion Jet wave should receive spell damage");
        helper.assertTrue(wave.getBurnDurationForGameTest() == 10,
                "Level one Combustion Jet should use its visual burn duration");

        wave.discard();
        owner.discard();
        helper.succeed();
    }

    static void combustionJetWaveHitsWideAreaOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "combustion_jet_width");
        var start = projectileTestStart(helper);
        var centerTarget = createZombie(level, start.add(1.5D, -0.5D, 0.0D));
        var sideTarget = createZombie(level, start.add(1.5D, -0.5D, 4.2D));
        var outsideTarget = createZombie(level, start.add(1.5D, -0.5D, 5.2D));
        var centerHealth = centerTarget.getHealth();
        var sideHealth = sideTarget.getHealth();
        var outsideHealth = outsideTarget.getHealth();

        var wave = createWave(level, owner, start, 4.0F, 40, 16.0F);
        wave.tick();

        helper.assertTrue(centerTarget.getHealth() < centerHealth && sideTarget.getHealth() < sideHealth,
                "Combustion Jet wave should damage targets across its seven-block width");
        helper.assertTrue(Math.abs(outsideTarget.getHealth() - outsideHealth) < VALUE_EPSILON,
                "Combustion Jet wave should not damage targets outside its width");
        helper.assertTrue(centerTarget.getDeltaMovement().x > 0.0D
                        && sideTarget.getDeltaMovement().x > 0.0D,
                "Combustion Jet wave should push targets in its travel direction");
        helper.assertTrue(centerTarget.getRemainingFireTicks() == 40
                        && sideTarget.getRemainingFireTicks() == 40,
                "Combustion Jet wave should ignite hit targets");
        helper.assertTrue(wave.getVictimCountForGameTest() == 2,
                "Combustion Jet wave should record each target once");

        var centerHealthAfterHit = centerTarget.getHealth();
        wave.tick();
        helper.assertTrue(Math.abs(centerTarget.getHealth() - centerHealthAfterHit) < VALUE_EPSILON,
                "One Combustion Jet wave should not hit the same target twice");

        discardAll(wave, centerTarget, sideTarget, outsideTarget, owner);
        helper.succeed();
    }

    static void combustionJetWaveAppliesEffectsWhenDamageFails(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "combustion_jet_effects");
        var start = projectileTestStart(helper);
        var shortBurnTarget = createZombie(level, start.add(1.5D, -0.5D, 0.0D));
        var longBurnTarget = createZombie(level, start.add(1.5D, -0.5D, 2.0D));
        shortBurnTarget.setInvulnerable(true);
        longBurnTarget.setInvulnerable(true);
        shortBurnTarget.setRemainingFireTicks(10);
        longBurnTarget.setRemainingFireTicks(80);
        var shortHealth = shortBurnTarget.getHealth();
        var longHealth = longBurnTarget.getHealth();

        var wave = createWave(level, owner, start, 4.0F, 40, 16.0F);
        wave.tick();

        helper.assertTrue(Math.abs(shortBurnTarget.getHealth() - shortHealth) < VALUE_EPSILON
                        && Math.abs(longBurnTarget.getHealth() - longHealth) < VALUE_EPSILON,
                "Combustion Jet test targets should reject damage while invulnerable");
        helper.assertTrue(shortBurnTarget.getDeltaMovement().x > 0.0D
                        && longBurnTarget.getDeltaMovement().x > 0.0D,
                "Combustion Jet should knock targets back even when damage fails");
        helper.assertTrue(shortBurnTarget.getRemainingFireTicks() == 40,
                "Combustion Jet should extend a shorter burn duration");
        helper.assertTrue(longBurnTarget.getRemainingFireTicks() == 80,
                "Combustion Jet should not shorten an existing burn duration");

        discardAll(wave, shortBurnTarget, longBurnTarget, owner);
        helper.succeed();
    }

    static void combustionJetWaveTargetsNonLivingCombatTarget(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "combustion_jet_non_living");
        var start = projectileTestStart(helper);
        var crystalPosition = start.add(1.5D, -0.5D, 0.0D);
        var crystal = new EndCrystal(level, crystalPosition.x, crystalPosition.y, crystalPosition.z);
        level.addFreshEntity(crystal);

        var wave = createWave(level, owner, start, 4.0F, 40, 16.0F);
        wave.tick();

        helper.assertTrue(wave.getVictimCountForGameTest() == 1,
                "Combustion Jet wave should target a valid non-living combat target");

        discardAll(wave, crystal, owner);
        helper.succeed();
    }

    static void combustionJetWaveUsesSmallBlockCollision(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "combustion_jet_terrain");
        var start = projectileTestStart(helper);
        var sideBlock = BlockPos.containing(start.add(0.0D, 0.0D, 1.0D));
        var centerBlock = BlockPos.containing(start.add(1.0D, 0.0D, 0.0D));

        level.setBlockAndUpdate(sideBlock, Blocks.STONE.defaultBlockState());
        var sidePassingWave = createWave(level, owner, start, 1.0F, 10, 16.0F);
        sidePassingWave.tick();
        helper.assertFalse(sidePassingWave.isRemoved(),
                "Combustion Jet side overlap should not count as terrain collision");

        level.setBlockAndUpdate(centerBlock, Blocks.STONE.defaultBlockState());
        var blockedWave = createWave(level, owner, start, 1.0F, 10, 16.0F);
        blockedWave.tick();
        helper.assertTrue(blockedWave.isRemoved(),
                "Combustion Jet center path should collide with terrain");

        level.setBlockAndUpdate(sideBlock, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(centerBlock, Blocks.AIR.defaultBlockState());
        discardAll(sidePassingWave, owner);
        helper.succeed();
    }

    static void combustionJetWaveDisappearsAtRange(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "combustion_jet_range");
        var start = projectileTestStart(helper);
        var targetBeyondRange = createZombie(level, start.add(5.0D, -0.5D, 0.0D));
        var targetHealth = targetBeyondRange.getHealth();
        var blockBeyondRange = BlockPos.containing(start.add(5.0D, 0.0D, 0.0D));
        level.setBlockAndUpdate(blockBeyondRange, Blocks.STONE.defaultBlockState());

        // 熱波の速度合わせにする.
        var wave = createWave(level, owner, start, 4.0F, 40, 2.25F);

        wave.tick();
        helper.assertTrue(wave.isRemoved(),
                "Combustion Jet wave should disappear immediately at its range");
        helper.assertTrue(Math.abs(wave.position().distanceTo(start) - 2.25D) < POSITION_EPSILON,
                "Combustion Jet wave should travel through its full range before disappearing");
        helper.assertTrue(Math.abs(targetBeyondRange.getHealth() - targetHealth) < VALUE_EPSILON,
                "Combustion Jet wave should not damage targets beyond its range");

        var antiMagicWave = createWave(level, owner, start, 1.0F, 10, 16.0F);
        antiMagicWave.onAntiMagic(MagicData.getPlayerMagicData(owner));
        helper.assertTrue(antiMagicWave.isRemoved(),
                "Combustion Jet wave should be removed by anti-magic");

        level.setBlockAndUpdate(blockBeyondRange, Blocks.AIR.defaultBlockState());
        discardAll(targetBeyondRange, owner);
        helper.succeed();
    }

    private static CombustionJetWaveEntity createWave(
            ServerLevel level, FakePlayer owner, Vec3 position,
            float damage, int burnDuration, float range
    ) {
        var wave = new CombustionJetWaveEntity(EntityRegistry.COMBUSTION_JET_WAVE.get(), level, owner);
        wave.setPos(position);
        wave.setDamage(damage);
        wave.setBurnDuration(burnDuration);
        wave.setMaxTravelDistance(range);
        wave.shoot(new Vec3(1.0D, 0.0D, 0.0D));
        level.addFreshEntity(wave);
        return wave;
    }

    private static Vec3 projectileTestStart(GameTestHelper helper) {
        return helper.absoluteVec(new Vec3(2.5D, PROJECTILE_TEST_HEIGHT, 2.5D));
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        var position = helper.absoluteVec(new Vec3(1.5D, 2.0D, 1.5D));
        player.setPos(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static Zombie createZombie(ServerLevel level, Vec3 position) {
        var zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            throw new IllegalStateException("Failed to create Combustion Jet test zombie");
        }
        zombie.setPos(position.x, position.y, position.z);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        return zombie;
    }

    private static void discardAll(net.minecraft.world.entity.Entity... entities) {
        for (var entity : entities) {
            entity.discard();
        }
    }
}
