package jp.aquafactory.apprenticecodex.gametest.spell.katana;

import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static jp.aquafactory.apprenticecodex.gametest.spell.higanbana.HiganbanaGameTestScenarios.HEALTH_EPSILON;
import static jp.aquafactory.apprenticecodex.gametest.spell.higanbana.HiganbanaGameTestScenarios.discard;
import static jp.aquafactory.apprenticecodex.gametest.spell.higanbana.KatanaAreaHitGameTestScenarios.*;

final class SlashBladeHitScenarios {
    private SlashBladeHitScenarios() {}

    static void slashBladeRefreshesPoseAndDamagesThroughWall(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "slash_blade_area_owner", new Vec3(2.5D, 2.0D, 2.5D));
        owner.setYRot(0.0F);
        var weapon = new SlashBladeKatanaEntity(EntityRegistry.SLASH_BLADE_KATANA.get(), level, owner);
        weapon.setDamage(4.0F);
        weapon.setBlockPenetrationDamageMultiplier(0.5F);
        level.addFreshEntity(weapon);

        var movedPosition = helper.absoluteVec(new Vec3(5.5D, 2.0D, 3.5D));
        owner.setPos(movedPosition.x, movedPosition.y, movedPosition.z);
        owner.setYRot(-90.0F);
        var expectedWeaponPosition = RotationTools.calculateBehindPosition(owner, -0.5D, 0.0D, -0.75D);
        var target = createZombie(level, expectedWeaponPosition.add(2.0D, 0.0D, 0.0D));
        var visibleTarget = createZombie(level, expectedWeaponPosition.add(0.5D, 0.0D, 2.0D));
        target.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
        visibleTarget.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
        var initialHealth = target.getHealth();
        var initialVisibleHealth = visibleTarget.getHealth();
        var initialMovement = new Vec3(0.125D, 0.0D, -0.25D);
        target.setDeltaMovement(initialMovement);
        for (var y = 1; y <= 4; ++y) {
            for (var z = 2; z <= 4; ++z) {
                helper.setBlock(new BlockPos(7, y, z), Blocks.STONE);
            }
        }

        var savedWeapon = new CompoundTag();
        weapon.saveWithoutId(savedWeapon);
        var restoredWeapon = new SlashBladeKatanaEntity(EntityRegistry.SLASH_BLADE_KATANA.get(), level);
        restoredWeapon.load(savedWeapon);
        helper.assertTrue(
                Math.abs(restoredWeapon.getBlockPenetrationDamageMultiplierForGameTest() - 0.5F)
                        < HEALTH_EPSILON,
                "Slash Blade should preserve its block penetration damage multiplier in NBT"
        );

        weapon.slash(level);

        helper.assertTrue(weapon.position().distanceTo(expectedWeaponPosition) < POSITION_EPSILON,
                "Slash Blade should refresh its position when the slash executes");
        helper.assertTrue(Math.abs(weapon.getYRot() - owner.getYRot()) < POSITION_EPSILON,
                "Slash Blade should refresh its horizontal direction when the slash executes");
        var blockPenetrationDamage = initialHealth - target.getHealth();
        var visibleDamage = initialVisibleHealth - visibleTarget.getHealth();
        helper.assertTrue(visibleDamage > HEALTH_EPSILON,
                "Slash Blade should damage the visible reference target");
        helper.assertTrue(
                Math.abs(blockPenetrationDamage - visibleDamage * 0.5F) < HEALTH_EPSILON,
                "Slash Blade should apply its block penetration damage multiplier through a wall"
                        + " (block penetration damage: " + blockPenetrationDamage
                        + ", visible damage: " + visibleDamage + ")");
        helper.assertTrue(target.getDeltaMovement().distanceTo(initialMovement) < POSITION_EPSILON,
                "Slash Blade block penetration damage should not knock the target back");

        discard(owner, weapon, restoredWeapon, target, visibleTarget);
        helper.succeed();
    }

    static void slashBladeKeepsFullDamageAndKnockbackWithoutWall(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "slash_blade_visible_target_owner", new Vec3(2.5D, 2.0D, 2.5D));
        owner.setYRot(0.0F);
        var weapon = new SlashBladeKatanaEntity(EntityRegistry.SLASH_BLADE_KATANA.get(), level, owner);
        weapon.setDamage(4.0F);
        weapon.setBlockPenetrationDamageMultiplier(0.5F);
        level.addFreshEntity(weapon);

        var target = createZombie(level, weapon.position().add(0.0D, 0.0D, 2.0D));
        var initialHealth = target.getHealth();
        var initialMovement = new Vec3(0.125D, 0.0D, -0.25D);
        target.setDeltaMovement(initialMovement);

        weapon.slash(level);

        helper.assertTrue(target.getHealth() < initialHealth - HEALTH_EPSILON,
                "Slash Blade should keep full damage against a visible target");
        helper.assertTrue(target.getDeltaMovement().distanceTo(initialMovement) > POSITION_EPSILON,
                "Slash Blade should keep default knockback against a visible target");

        discard(owner, weapon, target);
        helper.succeed();
    }
}
