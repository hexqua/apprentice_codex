package jp.aquafactory.apprenticecodex.gametest.spell.katana;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.damage.DamageTypes;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.higanbana.HiganbanaKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.slashblade.SlashBladeKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.thermalslice.ThermalSliceKatanaEntity;
import jp.aquafactory.apprenticecodex.utility.RotationTools;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.spell.katana.KatanaHitFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class KatanaOcclusionGameTests {
    private KatanaOcclusionGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void thermalSliceRejectsWallHitsAndDamagesVisibleTarget(GameTestHelper helper) {
        try (var scene = new KatanaHitFixture(helper)) {
            var level = scene.level;
            var owner = scene.player("thermal_slice_wall_owner", new Vec3(4.5D, 2.0D, 2.5D));
            owner.setYRot(0.0F);
            var weapon = new ThermalSliceKatanaEntity(EntityRegistry.THERMAL_SLICE_KATANA.get(), level, owner);
            weapon.setDamage(4.0F);
            scene.add(weapon);

            var target = scene.zombie(weapon.position().add(0.0D, 0.0D, 2.0D));
            target.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
            var initialHealth = target.getHealth();
            var wallCenter = BlockPos.containing(weapon.position().add(0.0D, 0.0D, 1.0D));
            for (var x = wallCenter.getX() - 1; x <= wallCenter.getX() + 1; ++x) {
                for (var y = wallCenter.getY(); y <= wallCenter.getY() + 2; ++y) {
                    scene.blockAbsolute(new BlockPos(x, y, wallCenter.getZ()), Blocks.STONE.defaultBlockState());
                }
            }

            scene.act(() -> weapon.slash(level));
            helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < HEALTH_EPSILON,
                    "Thermal Slice should reject a fully occluded target");

            for (var x = wallCenter.getX() - 1; x <= wallCenter.getX() + 1; ++x) {
                for (var y = wallCenter.getY(); y <= wallCenter.getY() + 2; ++y) {
                    scene.blockAbsolute(new BlockPos(x, y, wallCenter.getZ()), Blocks.AIR.defaultBlockState());
                }
            }
            var visibleWeapon = new ThermalSliceKatanaEntity(EntityRegistry.THERMAL_SLICE_KATANA.get(), level, owner);
            visibleWeapon.setDamage(4.0F);
            scene.add(visibleWeapon);
            scene.act(() -> visibleWeapon.slash(level));

            helper.assertTrue(target.getHealth() < initialHealth - HEALTH_EPSILON,
                    "Thermal Slice should damage a visible target");
            var damageSource = target.getLastDamageSource();
            helper.assertTrue(damageSource != null && damageSource.is(DamageTypes.THERMAL_SLICE),
                    "Thermal Slice should use its own damage type");

        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void higanbanaStaysHorizontalAndRejectsWallHits(GameTestHelper helper) {
        try (var scene = new KatanaHitFixture(helper)) {
            var level = scene.level;
            var owner = scene.player("higanbana_area_owner", new Vec3(2.5D, 3.0D, 2.5D));
            owner.setYRot(0.0F);
            owner.setXRot(60.0F);
            var weapon = new HiganbanaKatanaEntity(EntityRegistry.HIGANBANA_KATANA.get(), level, owner);
            weapon.setDamage(4.0F);
            weapon.setRemainingSlashCount(2);
            scene.add(weapon);

            var target = scene.zombie(weapon.position().add(0.0D, 0.0D, 2.0D));
            var initialHealth = target.getHealth();
            scene.block(new BlockPos(2, 3, 4), Blocks.STONE);
            scene.block(new BlockPos(2, 4, 4), Blocks.STONE);

            scene.act(() -> weapon.slash(level));
            helper.assertTrue(Math.abs(target.getHealth() - initialHealth) < HEALTH_EPSILON,
                    "Higanbana should not damage a fully occluded target");
            helper.assertTrue(Math.abs(weapon.getXRot()) < POSITION_EPSILON,
                    "Higanbana should remain horizontal regardless of owner pitch");

            scene.block(new BlockPos(2, 3, 4), Blocks.AIR);
            scene.block(new BlockPos(2, 4, 4), Blocks.AIR);
            scene.tick(weapon, 5);
            helper.assertTrue(target.getHealth() < initialHealth - HEALTH_EPSILON,
                    "Higanbana should reevaluate the wall and damage on a later slash");

        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void katanasKeepAttackOriginBeforeThinCover(GameTestHelper helper) {
        try (var scene = new KatanaHitFixture(helper)) {
            var level = scene.level;
            scene.thinPaneWall(2);
            scene.thinPaneWall(6);

            var slashOwner = scene.player("slash_blade_thin_cover_owner",
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
            scene.add(slashWeapon);
            var slashUnobstructedPosition =
                    RotationTools.calculateBehindPosition(slashOwner, -0.5D, 0.0D, -0.75D);
            helper.assertTrue(
                    slashWeapon.position().distanceTo(slashUnobstructedPosition) > POSITION_EPSILON,
                    "Slash Blade attack origin should stop before thin cover"
            );

            var slashTarget = scene.zombie(scene.position(new Vec3(2.5D, 2.0D, 4.0D)));
            slashTarget.getAttribute(Attributes.ARMOR).setBaseValue(0.0D);
            var slashInitialHealth = slashTarget.getHealth();
            scene.act(() -> slashWeapon.slash(level));
            var slashDamage = slashInitialHealth - slashTarget.getHealth();
            helper.assertTrue(
                    slashDamage > HEALTH_EPSILON && slashDamage < 4.0F - HEALTH_EPSILON,
                    "Slash Blade should apply reduced damage through thin cover"
            );

            var higanbanaOwner = scene.player("higanbana_thin_cover_owner",
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
            scene.add(higanbanaWeapon);
            var higanbanaUnobstructedPosition =
                    RotationTools.calculateBehindPosition(higanbanaOwner, -0.9D, 0.0D, -0.75D);
            helper.assertTrue(
                    higanbanaWeapon.position().distanceTo(higanbanaUnobstructedPosition) > POSITION_EPSILON,
                    "Higanbana attack origin should stop before thin cover"
            );

            var higanbanaTarget = scene.zombie(scene.position(new Vec3(6.5D, 2.0D, 4.0D)));
            var higanbanaInitialHealth = higanbanaTarget.getHealth();
            scene.act(() -> higanbanaWeapon.slash(level));
            helper.assertTrue(
                    Math.abs(higanbanaTarget.getHealth() - higanbanaInitialHealth) < HEALTH_EPSILON,
                    "Higanbana should reject a target behind thin cover"
            );

        }
        helper.succeed();
    }
}
