package jp.aquafactory.apprenticecodex.gametest.spell.katana;

import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.spell.shiden.ShidenKatanaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static jp.aquafactory.apprenticecodex.gametest.spell.higanbana.HiganbanaGameTestScenarios.HEALTH_EPSILON;
import static jp.aquafactory.apprenticecodex.gametest.spell.higanbana.HiganbanaGameTestScenarios.discard;
import static jp.aquafactory.apprenticecodex.gametest.spell.higanbana.KatanaAreaHitGameTestScenarios.*;

final class ShidenHitScenarios {
    private ShidenHitScenarios() {}

    static void shidenUsesTallNarrowAttackAreaAndCanBeInterrupted(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "shiden_area_owner", new Vec3(4.5D, 2.0D, 2.5D));
        owner.setYRot(0.0F);
        var weapon = new ShidenKatanaEntity(EntityRegistry.SHIDEN_KATANA.get(), level, owner);
        weapon.setDamage(4.0F);
        level.addFreshEntity(weapon);

        var origin = weapon.position();
        var centerTarget = createZombie(level, origin.add(0.0D, 0.0D, 2.0D));
        var upperTarget = createZombie(level, origin.add(0.0D, 1.25D, 3.0D));
        var lateralMiss = createZombie(level, origin.add(1.0D, 0.0D, 2.0D));
        var verticalMiss = createZombie(level, origin.add(0.0D, 1.9D, 2.0D));
        var depthMiss = createZombie(level, origin.add(0.0D, 0.0D, 5.0D));
        var rearMiss = createZombie(level, origin.add(0.0D, 0.0D, -0.8D));
        var targets = new Zombie[]{centerTarget, upperTarget, lateralMiss, verticalMiss, depthMiss, rearMiss};
        var initialHealth = new float[targets.length];
        for (var index = 0; index < targets.length; ++index) {
            targets[index].getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
            initialHealth[index] = targets[index].getHealth();
        }

        weapon.slash(level);

        helper.assertTrue(centerTarget.getHealth() < initialHealth[0] - HEALTH_EPSILON,
                "Shiden should hit a target in the center of its attack area");
        helper.assertTrue(upperTarget.getHealth() < initialHealth[1] - HEALTH_EPSILON,
                "Shiden should hit a target in the upper part of its three-block-high attack area");
        helper.assertTrue(Math.abs(lateralMiss.getHealth() - initialHealth[2]) < HEALTH_EPSILON,
                "Shiden should reject a target outside its one-block-wide attack area");
        helper.assertTrue(Math.abs(verticalMiss.getHealth() - initialHealth[3]) < HEALTH_EPSILON,
                "Shiden should reject a target above its three-block-high attack area");
        helper.assertTrue(Math.abs(depthMiss.getHealth() - initialHealth[4]) < HEALTH_EPSILON,
                "Shiden should reject a target beyond its 4.5-block attack depth");
        helper.assertTrue(Math.abs(rearMiss.getHealth() - initialHealth[5]) < HEALTH_EPSILON,
                "Shiden should reject a target behind its attack origin");
        helper.assertTrue(SpellRegistry.SHIDEN.get().canBeInterrupted(owner),
                "Shiden should remain interruptible when its caster is damaged");

        discard(owner, weapon, centerTarget, upperTarget, lateralMiss, verticalMiss, depthMiss, rearMiss);
        helper.succeed();
    }

    static void shidenReducesDamageAndKnockbackThroughWall(GameTestHelper helper) {
        var level = helper.getLevel();
        var blockedOwner = createPlayer(helper, "shiden_blocked_owner", new Vec3(2.5D, 2.0D, 2.5D));
        blockedOwner.setYRot(0.0F);
        var blockedWeapon = new ShidenKatanaEntity(EntityRegistry.SHIDEN_KATANA.get(), level, blockedOwner);
        blockedWeapon.setDamage(4.0F);
        blockedWeapon.setBlockPenetrationDamageMultiplier(0.5F);
        level.addFreshEntity(blockedWeapon);
        var blockedTarget = createZombie(level, blockedWeapon.position().add(0.0D, 0.0D, 2.0D));
        blockedTarget.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
        var blockedInitialHealth = blockedTarget.getHealth();
        var blockedInitialMovement = new Vec3(0.125D, 0.0D, -0.25D);
        blockedTarget.setDeltaMovement(blockedInitialMovement);
        var wallCenter = BlockPos.containing(blockedWeapon.position().add(0.0D, 0.0D, 1.0D));
        for (var x = wallCenter.getX() - 1; x <= wallCenter.getX() + 1; ++x) {
            for (var y = wallCenter.getY(); y <= wallCenter.getY() + 2; ++y) {
                level.setBlockAndUpdate(new BlockPos(x, y, wallCenter.getZ()), Blocks.STONE.defaultBlockState());
            }
        }

        var visibleOwner = createPlayer(helper, "shiden_visible_owner", new Vec3(6.5D, 2.0D, 2.5D));
        visibleOwner.setYRot(0.0F);
        var visibleWeapon = new ShidenKatanaEntity(EntityRegistry.SHIDEN_KATANA.get(), level, visibleOwner);
        visibleWeapon.setDamage(4.0F);
        visibleWeapon.setBlockPenetrationDamageMultiplier(0.5F);
        level.addFreshEntity(visibleWeapon);
        var visibleTarget = createZombie(level, visibleWeapon.position().add(0.0D, 0.0D, 2.0D));
        visibleTarget.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
        var visibleInitialHealth = visibleTarget.getHealth();
        var visibleInitialMovement = new Vec3(-0.125D, 0.0D, -0.25D);
        visibleTarget.setDeltaMovement(visibleInitialMovement);

        blockedWeapon.slash(level);
        visibleWeapon.slash(level);

        var blockedDamage = blockedInitialHealth - blockedTarget.getHealth();
        var visibleDamage = visibleInitialHealth - visibleTarget.getHealth();
        helper.assertTrue(visibleDamage > HEALTH_EPSILON,
                "Shiden should damage a visible target");
        helper.assertTrue(Math.abs(blockedDamage - visibleDamage * 0.5F) < HEALTH_EPSILON,
                "Shiden should apply its block penetration damage multiplier through a wall");
        helper.assertTrue(blockedTarget.getDeltaMovement().distanceTo(blockedInitialMovement) < POSITION_EPSILON,
                "Shiden block penetration damage should not knock the target back");
        helper.assertTrue(visibleTarget.getDeltaMovement().distanceTo(visibleInitialMovement) > POSITION_EPSILON,
                "Shiden should keep default knockback against a visible target");

        discard(blockedOwner, blockedWeapon, blockedTarget, visibleOwner, visibleWeapon, visibleTarget);
        helper.succeed();
    }
}
