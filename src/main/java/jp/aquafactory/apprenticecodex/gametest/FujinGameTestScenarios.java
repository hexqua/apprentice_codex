package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.fujin.Fujin;
import jp.aquafactory.apprenticecodex.spell.fujin.FujinKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.fujin.FujinSlashProjectileEntity;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

final class FujinGameTestScenarios {
    private static final double POSITION_EPSILON = 1.0E-5D;
    private static final float VALUE_EPSILON = 1.0E-4F;

    private FujinGameTestScenarios() {
    }

    static void fujinKatanaFollowsOwnerAndReleases(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "fujin_katana_timing");
        var spell = (Fujin) SpellRegistry.FUJIN.get();
        var katana = spell.onCastNoWeapon(level, 1, owner, MagicData.getPlayerMagicData(owner));

        owner.setPos(owner.position().add(2.0D, 1.0D, -1.0D));
        owner.setYRot(65.0F);
        owner.setXRot(-25.0F);

        katana.tickOnServer(level);
        assertKatanaFollowsOwner(helper, katana, owner);

        katana.releaseWeapon();
        helper.assertTrue(katana.isRemoved(), "Fujin katana should be removed when casting ends");
        owner.discard();
        helper.succeed();
    }

    static void fujinSlashPiercesAndDamagesEachTargetOnceWithoutKnockback(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "fujin_slash_damage");
        var start = helper.absoluteVec(new Vec3(2.5D, 3.0D, 2.5D));
        var firstTarget = createZombie(level, start.add(0.75D, 0.0D, 0.0D));
        var secondTarget = createZombie(level, start.add(1.75D, 0.0D, 0.0D));
        var firstMovement = new Vec3(0.125D, 0.0D, -0.25D);
        firstTarget.setDeltaMovement(firstMovement);
        var firstHealth = firstTarget.getHealth();
        var secondHealth = secondTarget.getHealth();

        var projectile = createProjectile(level, owner, start, 4.0F, 16.0F);
        projectile.tick();
        helper.assertTrue(firstTarget.getHealth() < firstHealth && secondTarget.getHealth() < secondHealth,
                "Fujin slash should pierce multiple targets");
        helper.assertTrue(projectile.getVictimCountForGameTest() == 2,
                "Fujin slash should record both resolved targets");
        helper.assertTrue(firstTarget.getDeltaMovement().distanceTo(firstMovement) < POSITION_EPSILON,
                "Fujin slash should not knock targets back");

        var firstHealthAfterHit = firstTarget.getHealth();
        projectile.tick();
        helper.assertTrue(Math.abs(firstTarget.getHealth() - firstHealthAfterHit) < VALUE_EPSILON,
                "One Fujin projectile should not damage the same target twice");
        helper.assertFalse(projectile.isRemoved(), "Fujin slash should remain after entity hits");

        projectile.discard();
        firstTarget.discard();
        secondTarget.discard();
        owner.discard();
        helper.succeed();
    }

    static void fujinSlashUsesSmallBlockCollision(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "fujin_slash_block_collision");
        var start = helper.absoluteVec(new Vec3(2.5D, 3.0D, 2.5D));
        var sideBlock = BlockPos.containing(start.add(0.0D, 0.0D, 1.0D));
        var centerBlock = BlockPos.containing(start);

        level.setBlockAndUpdate(sideBlock, Blocks.STONE.defaultBlockState());
        var sidePassingProjectile = createProjectile(level, owner, start, 1.0F, 16.0F);
        sidePassingProjectile.tick();
        helper.assertFalse(sidePassingProjectile.isRemoved(),
                "Fujin slash side overlap should not count as block collision");

        level.setBlockAndUpdate(centerBlock, Blocks.STONE.defaultBlockState());
        var blockedProjectile = createProjectile(level, owner, start, 1.0F, 16.0F);
        blockedProjectile.tick();
        helper.assertTrue(blockedProjectile.isRemoved(),
                "Fujin slash center path should collide with terrain immediately");

        level.setBlockAndUpdate(sideBlock, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(centerBlock, Blocks.AIR.defaultBlockState());
        sidePassingProjectile.discard();
        owner.discard();
        helper.succeed();
    }

    static void fujinSlashDamagesTargetBeforeWall(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "fujin_slash_target_before_wall");
        var start = helper.absoluteVec(new Vec3(2.5D, 3.0D, 2.5D));
        var target = createZombie(level, start.add(0.75D, 0.0D, 0.0D));
        var wallBlock = BlockPos.containing(start.add(2.0D, 0.0D, 0.0D));
        var healthBeforeHit = target.getHealth();

        level.setBlockAndUpdate(wallBlock, Blocks.STONE.defaultBlockState());
        var projectile = createProjectile(level, owner, start, 4.0F, 16.0F);
        projectile.tick();

        helper.assertTrue(projectile.isRemoved(), "Fujin slash should stop at terrain");
        helper.assertTrue(target.getHealth() < healthBeforeHit,
                "Fujin slash should damage targets before the blocking terrain");

        level.setBlockAndUpdate(wallBlock, Blocks.AIR.defaultBlockState());
        target.discard();
        owner.discard();
        helper.succeed();
    }

    static void fujinSlashExpiresBeyondRangeAndSupportsAntiMagic(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "fujin_slash_expiry");
        var start = helper.absoluteVec(new Vec3(2.5D, 3.0D, 2.5D));
        var projectile = createProjectile(level, owner, start, 1.0F, 16.0F);
        var stepDistance = projectile.getDeltaMovement().length();
        projectile.setMaxTravelDistance((float) (stepDistance * 2.0D));

        projectile.tick();
        helper.assertFalse(projectile.isRemoved(), "Fujin slash should remain before reaching range");
        for (var tick = 0; tick < 20 && !projectile.isRemoved(); ++tick) {
            projectile.tick();
        }
        helper.assertTrue(projectile.isRemoved(), "Fujin slash should eventually expire beyond its range");
        helper.assertTrue(projectile.position().distanceTo(start) > stepDistance * 2.0D,
                "Fujin slash should keep moving beyond its damage range while disappearing");

        var antiMagicProjectile = createProjectile(level, owner, start, 1.0F, 16.0F);
        antiMagicProjectile.onAntiMagic(MagicData.getPlayerMagicData(owner));
        helper.assertTrue(antiMagicProjectile.isRemoved(),
                "Fujin slash should be removed immediately by anti-magic");

        owner.discard();
        helper.succeed();
    }

    private static FujinSlashProjectileEntity createProjectile(
            ServerLevel level, FakePlayer owner, Vec3 position, float damage, float range
    ) {
        var projectile = new FujinSlashProjectileEntity(
                EntityRegistry.FUJIN_SLASH_PROJECTILE.get(), level, owner
        );
        projectile.setPos(position);
        projectile.setDamage(damage);
        projectile.setMaxTravelDistance(range);
        projectile.shoot(new Vec3(1.0D, 0.0D, 0.0D));
        level.addFreshEntity(projectile);
        return projectile;
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
            throw new IllegalStateException("Failed to create Fujin test zombie");
        }
        zombie.setPos(position.x, position.y, position.z);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        return zombie;
    }

    private static void assertKatanaFollowsOwner(
            GameTestHelper helper, FujinKatanaEntity katana, FakePlayer owner
    ) {
        var expected = RotationTools.calculateBehindPosition(owner, 0.0D, 0.0D, -0.75D);
        helper.assertTrue(katana.position().distanceTo(expected) < POSITION_EPSILON,
                "Fujin katana should follow its owner");
        helper.assertTrue(Math.abs(katana.getYRot() - owner.getYRot()) < VALUE_EPSILON
                        && Math.abs(katana.getXRot() - owner.getXRot()) < VALUE_EPSILON,
                "Fujin katana should follow owner yaw and pitch");
    }
}
