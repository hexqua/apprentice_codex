package jp.aquafactory.apprenticecodex.gametest.spell.katana;

import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.thermalslice.ThermalSliceKatanaEntity;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import static jp.aquafactory.apprenticecodex.gametest.spell.higanbana.HiganbanaGameTestScenarios.*;
import static jp.aquafactory.apprenticecodex.gametest.spell.higanbana.KatanaAreaHitGameTestScenarios.*;

final class KatanaOcclusionScenarios {
    private KatanaOcclusionScenarios() {}

    static void thermalSliceRejectsWallHitsAndDamagesVisibleTarget(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "thermal_slice_wall_owner", new Vec3(4.5D, 2.0D, 2.5D));
        owner.setYRot(0.0F);
        var weapon = new ThermalSliceKatanaEntity(EntityRegistry.THERMAL_SLICE_KATANA.get(), level, owner);
        weapon.setDamage(4.0F);
        level.addFreshEntity(weapon);

        var target = createZombie(level, weapon.position().add(0.0D, 0.0D, 2.0D));
        target.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
        var initialHealth = target.getHealth();
        var wallCenter = BlockPos.containing(weapon.position().add(0.0D, 0.0D, 1.0D));
        for (var x = wallCenter.getX() - 1; x <= wallCenter.getX() + 1; ++x) {
            for (var y = wallCenter.getY(); y <= wallCenter.getY() + 2; ++y) {
                level.setBlockAndUpdate(new BlockPos(x, y, wallCenter.getZ()), Blocks.STONE.defaultBlockState());
            }
        }

        weapon.slash(level);
        helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < HEALTH_EPSILON,
                "Thermal Slice should reject a fully occluded target");

        for (var x = wallCenter.getX() - 1; x <= wallCenter.getX() + 1; ++x) {
            for (var y = wallCenter.getY(); y <= wallCenter.getY() + 2; ++y) {
                level.setBlockAndUpdate(new BlockPos(x, y, wallCenter.getZ()), Blocks.AIR.defaultBlockState());
            }
        }
        var visibleWeapon = new ThermalSliceKatanaEntity(EntityRegistry.THERMAL_SLICE_KATANA.get(), level, owner);
        visibleWeapon.setDamage(4.0F);
        level.addFreshEntity(visibleWeapon);
        visibleWeapon.slash(level);

        helper.assertTrue(target.getHealth() < initialHealth - HEALTH_EPSILON,
                "Thermal Slice should damage a visible target");
        var damageSource = target.getLastDamageSource();
        helper.assertTrue(damageSource != null && damageSource.is(DamageTypes.THERMAL_SLICE),
                "Thermal Slice should use its own damage type");

        discard(owner, weapon, visibleWeapon, target);
        helper.succeed();
    }

    static void higanbanaStaysHorizontalAndRejectsWallHits(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "higanbana_area_owner", new Vec3(2.5D, 2.0D, 2.5D));
        owner.setYRot(0.0F);
        owner.setXRot(60.0F);
        var weapon = new HiganbanaKatanaEntity(EntityRegistry.HIGANBANA_KATANA.get(), level, owner);
        weapon.setDamage(4.0F);
        weapon.setRemainingSlashCount(2);
        level.addFreshEntity(weapon);

        var target = createZombie(level, weapon.position().add(0.0D, 0.0D, 2.0D));
        var initialHealth = target.getHealth();
        helper.setBlock(new BlockPos(2, 2, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 3, 4), Blocks.STONE);

        weapon.slash(level);
        helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < HEALTH_EPSILON,
                "Higanbana should not damage a fully occluded target");
        helper.assertTrue(Math.abs(weapon.getXRot()) < POSITION_EPSILON,
                "Higanbana should remain horizontal regardless of owner pitch");

        helper.setBlock(new BlockPos(2, 2, 4), Blocks.AIR);
        helper.setBlock(new BlockPos(2, 3, 4), Blocks.AIR);
        tickWeapon(level, weapon, 5);
        helper.assertTrue(target.getHealth() < initialHealth - HEALTH_EPSILON,
                "Higanbana should reevaluate the wall and damage on a later slash");

        discard(owner, weapon, target);
        helper.succeed();
    }

    static void katanasKeepAttackOriginBeforeThinCover(GameTestHelper helper) {
        var level = helper.getLevel();
        setThinPaneWall(helper, 2);
        setThinPaneWall(helper, 6);

        var slashOwner = createPlayer(
                helper,
                "slash_blade_thin_cover_owner",
                new Vec3(2.5D, 2.0D, 2.1D)
        );
        slashOwner.setYRot(0.0F);
        var slashWeapon = new SlashBladeKatanaEntity(
                EntityRegistry.SLASH_BLADE_KATANA.get(),
                level,
                slashOwner
        );
        slashWeapon.setDamage(4.0F);
        slashWeapon.setBlockPenetrationDamageMultiplier(0.5F);
        level.addFreshEntity(slashWeapon);
        var slashUnobstructedPosition =
                RotationTools.calculateBehindPosition(slashOwner, -0.5D, 0.0D, -0.75D);
        helper.assertTrue(
                slashWeapon.position().distanceTo(slashUnobstructedPosition) > POSITION_EPSILON,
                "Slash Blade attack origin should stop before thin cover"
        );

        var slashTarget = createZombie(level, helper.absoluteVec(new Vec3(2.5D, 2.0D, 4.0D)));
        slashTarget.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
        var slashInitialHealth = slashTarget.getHealth();
        slashWeapon.slash(level);
        var slashDamage = slashInitialHealth - slashTarget.getHealth();
        helper.assertTrue(
                slashDamage > HEALTH_EPSILON && slashDamage < 4.0F - HEALTH_EPSILON,
                "Slash Blade should apply reduced damage through thin cover"
        );

        var higanbanaOwner = createPlayer(
                helper,
                "higanbana_thin_cover_owner",
                new Vec3(6.5D, 2.0D, 2.1D)
        );
        higanbanaOwner.setYRot(0.0F);
        var higanbanaWeapon = new HiganbanaKatanaEntity(
                EntityRegistry.HIGANBANA_KATANA.get(),
                level,
                higanbanaOwner
        );
        higanbanaWeapon.setDamage(4.0F);
        higanbanaWeapon.setRemainingSlashCount(1);
        level.addFreshEntity(higanbanaWeapon);
        var higanbanaUnobstructedPosition =
                RotationTools.calculateBehindPosition(higanbanaOwner, -0.9D, 0.0D, -0.75D);
        helper.assertTrue(
                higanbanaWeapon.position().distanceTo(higanbanaUnobstructedPosition) > POSITION_EPSILON,
                "Higanbana attack origin should stop before thin cover"
        );

        var higanbanaTarget = createZombie(level, helper.absoluteVec(new Vec3(6.5D, 2.0D, 4.0D)));
        var higanbanaInitialHealth = higanbanaTarget.getHealth();
        higanbanaWeapon.slash(level);
        helper.assertTrue(
                Math.abs(higanbanaTarget.getHealth() - higanbanaInitialHealth) < HEALTH_EPSILON,
                "Higanbana should reject a target behind thin cover"
        );

        discard(
                slashOwner,
                slashWeapon,
                slashTarget,
                higanbanaOwner,
                higanbanaWeapon,
                higanbanaTarget
        );
        helper.succeed();
    }
}
