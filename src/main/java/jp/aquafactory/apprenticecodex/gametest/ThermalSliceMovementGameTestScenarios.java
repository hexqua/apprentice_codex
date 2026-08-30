package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.capability.Capabilities;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.CodexSpellStateTypeRegister;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates.ThermalSliceState;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.thermalslice.ThermalSliceKatanaEntity;
import jp.aquafactory.apprenticecodex.spell.thermalslice.ThermalSliceMovementEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

final class ThermalSliceMovementGameTestScenarios {
    private static final double POSITION_EPSILON = 1.0E-4D;
    private static final float HEALTH_EPSILON = 1.0E-4F;
    private static final double MAX_SUPPORTED_DASH_DISTANCE = 5.0D;

    private ThermalSliceMovementGameTestScenarios() {
    }

    static void thermalSliceDashesFixedDistanceAndCanOvershootCloseTarget(GameTestHelper helper) {
        var level = helper.getLevel();
        var startPosition = new Vec3(3.5D, 2.0D, 1.5D);
        clearPositiveZDashCorridor(helper, startPosition);
        var owner = createPlayer(helper, "thermal_slice_fixed_dash_owner", startPosition);
        owner.setYRot(0.0F);
        owner.setNoGravity(true);
        var weapon = createWeapon(level, owner);
        var closeTarget = createZombie(level, owner.position().add(0.0D, 0.0D, 1.0D));
        var initialHealth = closeTarget.getHealth();
        var start = owner.position();

        helper.assertTrue(ThermalSliceMovementEvent.startDash(owner, weapon.getId()),
                "Thermal Slice should start its dash for a valid player");
        for (var tick = 0; tick < ThermalSliceMovementEvent.DASH_DURATION_TICKS - 1; ++tick) {
            tickDash(owner);
            helper.assertFalse(weapon.isSlashed(), "Thermal Slice should not slash before the dash finishes");
        }
        tickDash(owner);

        var horizontalDistance = owner.position().multiply(1.0D, 0.0D, 1.0D)
                .distanceTo(start.multiply(1.0D, 0.0D, 1.0D));
        helper.assertTrue(Math.abs(horizontalDistance - ThermalSliceMovementEvent.DASH_DISTANCE) <= POSITION_EPSILON,
                "Thermal Slice should dash the configured fixed distance: expected="
                        + ThermalSliceMovementEvent.DASH_DISTANCE + ", actual=" + horizontalDistance);
        helper.assertTrue(weapon.isSlashed(), "Thermal Slice should slash when the dash finishes");
        helper.assertTrue(Math.abs(closeTarget.getHealth() - initialHealth) <= HEALTH_EPSILON,
                "Thermal Slice should be able to overshoot a target that was too close");
        helper.assertFalse(getState(owner).isActive(), "Thermal Slice should clear its movement state after slashing");

        discard(owner, weapon, closeTarget);
        helper.succeed();
    }

    private static void clearPositiveZDashCorridor(GameTestHelper helper, Vec3 start) {
        var minX = (int) Math.floor(start.x - 1.0D);
        var maxX = (int) Math.ceil(start.x + 1.0D);
        var minY = (int) Math.floor(start.y);
        var maxY = (int) Math.ceil(start.y + 2.0D);
        var minZ = (int) Math.floor(start.z);
        var maxZ = (int) Math.ceil(start.z + MAX_SUPPORTED_DASH_DISTANCE + 1.0D);
        for (var x = minX; x <= maxX; ++x) {
            for (var y = minY; y <= maxY; ++y) {
                for (var z = minZ; z <= maxZ; ++z) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    static void thermalSliceStopsAtWallAndSlashesEarly(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "thermal_slice_wall_dash_owner", new Vec3(7.5D, 2.0D, 2.5D));
        owner.setYRot(0.0F);
        owner.setNoGravity(true);
        var weapon = createWeapon(level, owner);
        var start = owner.position();
        for (var x = 6; x <= 8; ++x) {
            for (var y = 2; y <= 3; ++y) {
                helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
            }
        }
        var target = createZombie(level, helper.absoluteVec(new Vec3(7.5D, 2.0D, 5.0D)));
        var initialHealth = target.getHealth();

        helper.assertTrue(ThermalSliceMovementEvent.startDash(owner, weapon.getId()),
                "Thermal Slice should start before colliding with a wall");
        tickDash(owner);

        helper.assertTrue(owner.position().distanceTo(start) < 0.5D,
                "Thermal Slice should stop before completing its first dash step into a wall");
        helper.assertTrue(weapon.isSlashed(), "Thermal Slice should slash immediately after horizontal collision");
        helper.assertTrue(Math.abs(target.getHealth() - initialHealth) <= HEALTH_EPSILON,
                "Thermal Slice should not damage a target behind the blocking wall");
        helper.assertFalse(getState(owner).isActive(), "Thermal Slice should clear its state after a blocked dash");

        discard(owner, weapon, target);
        helper.succeed();
    }

    static void thermalSliceKeepsDashDirectionButUsesFinalLookForSlash(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "thermal_slice_turning_owner", new Vec3(7.5D, 2.0D, 1.5D));
        owner.setYRot(0.0F);
        owner.setNoGravity(true);
        var weapon = createWeapon(level, owner);
        var start = owner.position();
        var expectedEnd = start.add(0.0D, 0.0D, ThermalSliceMovementEvent.DASH_DISTANCE);

        helper.assertTrue(ThermalSliceMovementEvent.startDash(owner, weapon.getId()),
                "Thermal Slice should start before the player turns");
        for (var tick = 0; tick < ThermalSliceMovementEvent.DASH_DURATION_TICKS - 1; ++tick) {
            tickDash(owner);
        }
        owner.setYRot(90.0F);
        tickDash(owner);

        helper.assertTrue(Math.abs(owner.getX() - start.x) <= POSITION_EPSILON,
                "Turning should not redirect the active Thermal Slice dash");
        helper.assertTrue(Math.abs(owner.getZ() - expectedEnd.z) <= POSITION_EPSILON,
                "Thermal Slice should finish along its initial dash direction: expectedZ="
                        + expectedEnd.z + ", actualZ=" + owner.getZ());
        helper.assertTrue(weapon.isSlashed(), "Thermal Slice should slash when the dash finishes");
        helper.assertTrue(Math.abs(weapon.getYRot() - owner.getYRot()) <= POSITION_EPSILON,
                "Thermal Slice should face the player's final look direction when slashing");

        discard(owner, weapon);
        helper.succeed();
    }

    static void thermalSlicePreservesVerticalMovementAndFallDistance(GameTestHelper helper) {
        var level = helper.getLevel();
        var owner = createPlayer(helper, "thermal_slice_air_dash_owner", new Vec3(3.5D, 6.0D, 7.5D));
        owner.setYRot(0.0F);
        var weapon = createWeapon(level, owner);
        owner.setDeltaMovement(0.0D, -0.25D, 0.0D);
        owner.fallDistance = 4.0F;

        helper.assertTrue(ThermalSliceMovementEvent.startDash(owner, weapon.getId()),
                "Thermal Slice should start while airborne");
        ThermalSliceMovementEvent.onPlayerTick(new PlayerTickEvent.Pre(owner));

        helper.assertTrue(Math.abs(owner.getDeltaMovement().y + 0.25D) <= POSITION_EPSILON,
                "Thermal Slice should preserve vertical movement");
        helper.assertTrue(Math.abs(owner.fallDistance - 4.0F) <= POSITION_EPSILON,
                "Thermal Slice should not reset fall distance");
        helper.assertFalse(owner.isNoGravity(), "Thermal Slice should not disable gravity");

        discard(owner, weapon);
        helper.succeed();
    }

    private static void tickDash(FakePlayer player) {
        ThermalSliceMovementEvent.onPlayerTick(new PlayerTickEvent.Pre(player));
        player.move(MoverType.SELF, player.getDeltaMovement());
        ThermalSliceMovementEvent.onPlayerTick(new PlayerTickEvent.Post(player));
    }

    private static ThermalSliceState getState(FakePlayer player) {
        return Capabilities.getSpellData(player).orElseThrow()
                .get(CodexSpellStateTypeRegister.THERMAL_SLICE_STATE);
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name, Vec3 localPosition) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
        var position = helper.absoluteVec(localPosition);
        player.setPos(position.x, position.y, position.z);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static ThermalSliceKatanaEntity createWeapon(net.minecraft.server.level.ServerLevel level, FakePlayer owner) {
        var weapon = new ThermalSliceKatanaEntity(EntityRegistry.THERMAL_SLICE_KATANA.get(), level, owner);
        weapon.setDamage(4.0F);
        level.addFreshEntity(weapon);
        return weapon;
    }

    private static Zombie createZombie(net.minecraft.server.level.ServerLevel level, Vec3 position) {
        var zombie = EntityType.ZOMBIE.create(level);
        if (zombie == null) {
            throw new IllegalStateException("Failed to create Thermal Slice movement test zombie");
        }
        zombie.setPos(position.x, position.y, position.z);
        zombie.setNoAi(true);
        level.addFreshEntity(zombie);
        return zombie;
    }

    private static void discard(Entity... entities) {
        for (var entity : entities) {
            entity.discard();
        }
    }
}
