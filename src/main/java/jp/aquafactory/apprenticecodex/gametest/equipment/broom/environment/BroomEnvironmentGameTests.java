package jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.*;
import jp.aquafactory.apprenticecodex.gametest.crafting.calibration.SpellCalibrationAdjustmentGameTestSupport;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.control.BroomPresentationGameTests.assertTooltipLine;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BroomEnvironmentGameTests {
    private BroomEnvironmentGameTests() {}

    public static final String TEMPLATE = "gametest/basic_floor";





    @GameTest(template = TEMPLATE)
    public static void floatmountWaterMovementLimitsUseSpecifiedFactors(GameTestHelper helper) {
        helper.assertTrue(Math.abs(FloatmountBroomMovement.horizontalAcceleration(true) - 0.02D) < 1.0e-9D,
                "Water should halve Floatmount horizontal acceleration");
        helper.assertTrue(Math.abs(FloatmountBroomMovement.maximumHorizontalSpeed(false, true) - 0.21D) < 1.0e-9D,
                "Water should reduce Floatmount maximum horizontal speed to sixty percent");
        helper.assertTrue(Math.abs(FloatmountBroomMovement.maximumHorizontalSpeed(true, true) - 0.06D) < 1.0e-9D,
                "Water should reduce emergency maximum horizontal speed to sixty percent");
        helper.assertTrue(Math.abs(FloatmountBroomMovement.maximumVerticalSpeed(true) - 0.09D) < 1.0e-9D,
                "Water should reduce Floatmount vertical speed to sixty percent");
        helper.assertTrue(Math.abs(FloatmountBroomMovement.maximumUnoccupiedVerticalSpeed(true) - 0.06D) < 1.0e-9D,
                "Water should reduce unoccupied Floatmount rise and fall speed to sixty percent");
        helper.assertTrue(Math.abs(
                        FloatmountBroomMovement.horizontalAcceleration(false, true) - 0.056D
                ) < 1.0e-9D,
                "Overdrive should increase Floatmount horizontal acceleration by forty percent");
        helper.assertTrue(Math.abs(
                        FloatmountBroomMovement.maximumHorizontalSpeed(false, false, true) - 0.49D
                ) < 1.0e-9D,
                "Overdrive should increase Floatmount maximum horizontal speed by forty percent");
        helper.assertTrue(Math.abs(
                        FloatmountBroomMovement.maximumHorizontalSpeed(false, true, true) - 0.294D
                ) < 1.0e-9D,
                "Water penalty should apply after the Floatmount Overdrive speed increase");
        helper.assertTrue(Math.abs(
                        FloatmountBroomMovement.maximumHorizontalSpeed(true, false, true) - 0.1D
                ) < 1.0e-9D,
                "Overdrive must not increase Floatmount emergency landing speed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideUnoccupiedBroomHoversOverLava(GameTestHelper helper) {
        var lava = helper.absolutePos(TEST_POS);
        helper.getLevel().setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
        var broom = spawnHoverrideBroom(helper, 1.6D);
        var startY = broom.getY();
        for (var tick = 0; tick < 6; ++tick) {
            broom.tick();
        }
        helper.assertTrue(broom.getY() > startY,
                "An unoccupied Hoverride Broom should rise toward its hover height above lava");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideLavaSurfacePreventsAirborneLock(GameTestHelper helper) {
        var lava = helper.absolutePos(TEST_POS);
        helper.getLevel().setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
        var broom = spawnHoverrideBroom(helper, 2.2D);
        var player = serverRider(helper);
        magicData(helper, player).setMana(100.0F);
        helper.assertTrue(player.startRiding(broom, true), "Hoverride lava test rider should mount");
        for (var tick = 0; tick < 40; ++tick) {
            broom.tick();
        }
        helper.assertFalse(broom.isAirborneAccelerationLocked(),
                "Lava below an Hoverride Broom should prevent the airborne acceleration lock");
        helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.NONE,
                "Lava-supported Hoverride flight should keep the normal core warning state");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverrideTooltipIncludesAssistWingsAndRecoveryDescriptions(GameTestHelper helper) {
        var stack = new ItemStack(ItemRegistry.HOVERRIDE_BROOM.get());
        var lines = new ArrayList<Component>();
        stack.getItem().appendHoverText(
                stack, helper.getLevel(), lines, TooltipFlag.Default.NORMAL
        );

        helper.assertTrue(lines.size() == 5, "Hoverride Broom should have five tooltip lines");
        assertTooltipLine(helper, lines, 0, "item.apprenticecodex.hoverride_broom.desc_1", 1);
        assertTooltipLine(helper, lines, 1, "item.apprenticecodex.hoverride_broom.desc_2", 2);
        assertTooltipLine(helper, lines, 2, "item.apprenticecodex.broom.desc_calibration", 0);
        assertTooltipLine(helper, lines, 3, "item.apprenticecodex.hoverride_broom.desc_3", 0);
        assertTooltipLine(helper, lines, 4, "item.apprenticecodex.hoverride_broom.desc_4", 0);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void broomMovementChecksBubbleColumnsOncePerTick(GameTestHelper helper) {
        var level = helper.getLevel();
        var bubble = helper.absolutePos(new BlockPos(1, 1, 1));
        var water = helper.absolutePos(new BlockPos(3, 1, 1));
        installBubbleColumn(level, bubble, 3);
        installWaterColumn(level, water, 3);

        var floatmountBubble = spawnBroomAt(helper, bubble);
        var floatmountWater = spawnBroomAt(helper, water);
        floatmountBubble.tick();
        floatmountWater.tick();
        helper.assertTrue(Math.abs(
                        floatmountBubble.getDeltaMovement().y - floatmountWater.getDeltaMovement().y - 0.06D
                ) < 1.0e-6D,
                "Floatmount Broom should receive one inside-bubble-column impulse per movement tick");
        floatmountBubble.discard();
        floatmountWater.discard();

        var hoverrideBubble = spawnHoverrideBroomAt(helper, bubble);
        var hoverrideWater = spawnHoverrideBroomAt(helper, water);
        hoverrideBubble.tick();
        hoverrideWater.tick();
        helper.assertTrue(Math.abs(
                        hoverrideBubble.getDeltaMovement().y - hoverrideWater.getDeltaMovement().y - 0.06D
                ) < 1.0e-6D,
                "Hoverride Broom should receive one inside-bubble-column impulse per movement tick");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aquaticCalibrationIgnoresOnlyBroomBubbleMovement(GameTestHelper helper) {
        var level = helper.getLevel();
        var bubble = helper.absolutePos(new BlockPos(1, 1, 1));
        var water = helper.absolutePos(new BlockPos(3, 1, 1));
        // 高さのある同一条件の列で、気泡callbackの移動成分だけが除外されることを検証する。
        installBubbleColumn(level, bubble, 8);
        installWaterColumn(level, water, 8);

        var calibratedStack = new ItemStack(ItemRegistry.FLOATMOUNT_BROOM.get());
        helper.assertTrue(SpellCalibrationAdjustmentGameTestSupport.setCalibrationAdjustment(
                        calibratedStack, 0, new ItemStack(Items.HEART_OF_THE_SEA)),
                "Aquatic calibration setup should accept Heart of the Sea");
        var bubbleBroom = spawnBroomAt(helper, bubble);
        var waterBroom = spawnBroomAt(helper, water);
        bubbleBroom.setBroomItemStack(calibratedStack);
        waterBroom.setBroomItemStack(calibratedStack);
        bubbleBroom.tick();
        waterBroom.tick();
        helper.assertTrue(Math.abs(
                        bubbleBroom.getDeltaMovement().y - waterBroom.getDeltaMovement().y
                ) < 1.0e-6D,
                "Aquatic calibration should ignore bubble-column movement on the broom");
        bubbleBroom.fallDistance = 5.0F;
        var beforeCallback = bubbleBroom.getDeltaMovement();
        bubbleBroom.onInsideBubbleColumn(false);
        helper.assertTrue(bubbleBroom.getDeltaMovement().equals(beforeCallback),
                "Aquatic calibration should restore only the broom velocity changed by the callback");
        helper.assertTrue(bubbleBroom.fallDistance == 0.0F,
                "Aquatic calibration should preserve non-movement effects from the bubble callback");
        helper.assertTrue(level.getBlockState(bubble).is(Blocks.BUBBLE_COLUMN),
                "Aquatic calibration must leave the bubble column available to the rider");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void waterPenaltyClampsUnoccupiedAndDismountMovement(GameTestHelper helper) {
        var level = helper.getLevel();
        var water = helper.absolutePos(new BlockPos(1, 1, 1));
        installWaterColumn(level, water, 3);

        var unoccupied = spawnBroomAt(helper, water);
        unoccupied.setDeltaMovement(0.4D, 0.0D, 0.0D);
        var unoccupiedStartX = unoccupied.getX();
        unoccupied.tick();
        var unoccupiedDisplacement = unoccupied.getX() - unoccupiedStartX;
        helper.assertTrue(unoccupiedDisplacement > 0.0D && unoccupiedDisplacement <= 0.21D + 1.0e-6D,
                "Unoccupied Floatmount Broom should move at most 0.21 blocks per water tick, actual: "
                        + unoccupiedDisplacement);
        unoccupied.discard();

        var falling = spawnBroomAt(helper, water.above());
        falling.setDeltaMovement(0.0D, -0.2D, 0.0D);
        var fallingStartY = falling.getY();
        falling.tick();
        var fallingDisplacement = falling.getY() - fallingStartY;
        helper.assertTrue(Math.abs(fallingDisplacement + 0.06D) < 1.0e-6D,
                "Unoccupied Floatmount Broom should clamp water fall speed to 0.06, actual: "
                        + fallingDisplacement);
        falling.discard();

        var ridden = spawnBroomAt(helper, water);
        var player = serverRider(helper, "water_dismount_clamp_test");
        player.getAbilities().instabuild = true;
        helper.assertTrue(player.startRiding(ridden, true), "Water dismount test rider should mount");
        ridden.tick();
        ridden.setPos(ridden.getX() + 0.4D, ridden.getY() + 0.2D, ridden.getZ());
        ridden.tick();
        player.stopRiding();
        helper.assertTrue(Math.abs(ridden.getDeltaMovement().horizontalDistance() - 0.21D) < 1.0e-6D,
                "Dismounted Floatmount Broom should clamp inherited horizontal water speed to 0.21");
        helper.assertTrue(Math.abs(ridden.getDeltaMovement().y - 0.09D) < 1.0e-6D,
                "Dismounted Floatmount Broom should clamp inherited vertical water speed to 0.09");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void damagedUnoccupiedBroomRisesOutOfLava(GameTestHelper helper) {
        var level = helper.getLevel();
        var lava = helper.absolutePos(TEST_POS);
        level.setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());
        level.setBlockAndUpdate(lava.above(), Blocks.LAVA.defaultBlockState());
        level.setBlockAndUpdate(lava.above(2), Blocks.LAVA.defaultBlockState());

        var broom = spawnBroom(helper, 0.2D);
        broom.hurt(level.damageSources().fellOutOfWorld(), 20.0F);
        broom.setDeltaMovement(0.0D, -0.15D, 0.0D);
        var startY = broom.getY();

        helper.runAfterDelay(12, () -> {
            helper.assertTrue(broom.isDamaged(), "Lava recovery must not clear the damaged state");
            helper.assertFalse(broom.isVehicle(), "Lava recovery test broom must remain unoccupied");
            helper.assertTrue(broom.getDeltaMovement().y > 0.0D,
                    "Damaged unoccupied broom should reverse its downward motion while in lava");
            helper.assertTrue(broom.getY() > startY,
                    "Damaged unoccupied broom should rise toward the lava surface");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void surfaceScannerTreatsSolidWaterLavaAndForcedBlocksAsHoverSurfaces(GameTestHelper helper) {
        var level = helper.getLevel();
        var solid = helper.absolutePos(new BlockPos(1, 1, 1));
        var water = helper.absolutePos(new BlockPos(2, 1, 1));
        var powderSnow = helper.absolutePos(new BlockPos(3, 1, 1));
        var lava = helper.absolutePos(new BlockPos(4, 1, 1));
        level.setBlockAndUpdate(solid, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(powderSnow.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(powderSnow, Blocks.POWDER_SNOW.defaultBlockState());
        level.setBlockAndUpdate(lava.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(lava, Blocks.LAVA.defaultBlockState());

        helper.assertTrue(BroomSurfaceScanner.findSurfaceBelow(
                level, solid.getX() + 0.5D, solid.getY() + 2.0D, solid.getZ() + 0.5D, 3, true).isPresent(),
                "Solid collision shape should be a hover surface");
        helper.assertTrue(BroomSurfaceScanner.findSurfaceBelow(
                level, water.getX() + 0.5D, water.getY() + 2.0D, water.getZ() + 0.5D, 3, true).isPresent(),
                "Water should be a hover surface");
        var powderSnowSurface = BroomSurfaceScanner.findSurfaceBelow(
                level,
                powderSnow.getX() + 0.5D,
                powderSnow.getY() + 2.0D,
                powderSnow.getZ() + 0.5D,
                3,
                true
        ).orElseThrow();
        helper.assertTrue(powderSnowSurface.blockPos().equals(powderSnow),
                "Forced hover surface should preserve the tagged block position");
        helper.assertTrue(Math.abs(powderSnowSurface.y() - (powderSnow.getY() + 1.0D)) < 1.0E-5D,
                "Powder snow should use its own upper face instead of the solid block below");
        helper.assertTrue(BroomSurfaceScanner.findSurfaceBelow(
                level, lava.getX() + 0.5D, lava.getY() + 2.0D, lava.getZ() + 0.5D, 3, true).isPresent(),
                "Lava should be a hover surface");
        helper.assertTrue(BroomSurfaceScanner.findSurfaceBelow(
                        level, lava.getX() + 0.5D, lava.getY() + 2.0D, lava.getZ() + 0.5D, 3, false).isEmpty(),
                "Lava must make the safe dismount scan fail even with solid ground beneath it");

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(lava.getX() + 0.5D, lava.getY() + 1.5D, lava.getZ() + 0.5D);
        helper.assertTrue(broom.isDangerousDismount(),
                "Lava below the broom must always be classified as a dangerous dismount surface");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverridePowderSnowSurfacePreventsAirborneLock(GameTestHelper helper) {
        var powderSnowBase = helper.absolutePos(TEST_POS);
        for (var height = 0; height < 5; ++height) {
            helper.getLevel().setBlockAndUpdate(
                    powderSnowBase.above(height),
                    Blocks.POWDER_SNOW.defaultBlockState()
            );
        }
        var powderSnowTop = powderSnowBase.above(4);
        var broom = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), helper.getLevel());
        broom.setPos(
                powderSnowTop.getX() + 0.5D,
                powderSnowTop.getY() + 2.2D,
                powderSnowTop.getZ() + 0.5D
        );
        helper.getLevel().addFreshEntity(broom);
        var player = serverRider(helper, "hoverride_powder_snow_surface");
        magicData(helper, player).setMana(100.0F);
        helper.assertTrue(player.startRiding(broom, true), "Hoverride powder snow test rider should mount");

        for (var tick = 0; tick < 40; ++tick) {
            broom.tick();
        }

        helper.assertFalse(broom.isAirborneAccelerationLocked(),
                "Tagged powder snow below an Hoverride Broom should prevent the airborne acceleration lock");
        helper.assertTrue(broom.getCoreWarningState() == BroomCoreWarningState.NONE,
                "Powder snow-supported Hoverride flight should keep the normal core warning state");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unoccupiedBroomsRiseTowardPowderSnowSurface(GameTestHelper helper) {
        var level = helper.getLevel();
        var floatmountSurface = helper.absolutePos(new BlockPos(1, 1, 2));
        var hoverrideSurface = helper.absolutePos(new BlockPos(3, 1, 2));
        level.setBlockAndUpdate(floatmountSurface, Blocks.POWDER_SNOW.defaultBlockState());
        level.setBlockAndUpdate(hoverrideSurface, Blocks.POWDER_SNOW.defaultBlockState());

        var floatmount = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        floatmount.setPos(
                floatmountSurface.getX() + 0.5D,
                floatmountSurface.getY() + 1.2D,
                floatmountSurface.getZ() + 0.5D
        );
        level.addFreshEntity(floatmount);
        var hoverride = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), level);
        hoverride.setPos(
                hoverrideSurface.getX() + 0.5D,
                hoverrideSurface.getY() + 1.2D,
                hoverrideSurface.getZ() + 0.5D
        );
        level.addFreshEntity(hoverride);
        var floatmountStartY = floatmount.getY();
        var hoverrideStartY = hoverride.getY();

        for (var tick = 0; tick < 6; ++tick) {
            floatmount.tick();
            hoverride.tick();
        }

        helper.assertTrue(floatmount.getY() > floatmountStartY,
                "Unoccupied Floatmount Broom should rise toward the powder snow hover height");
        helper.assertTrue(hoverride.getY() > hoverrideStartY,
                "Unoccupied Hoverride Broom should rise toward the powder snow hover height");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void safeDismountUsesBroomLeftSurfaceInsteadOfPassengerView(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 1, 2));
        var leftGround = center.east();
        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(3), Blocks.AIR.defaultBlockState());

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(center.getX() + 0.5D, center.getY() + 2.5D, center.getZ() + 0.5D);
        broom.setYRot(0.0F);
        level.addFreshEntity(broom);
        var player = serverRider(helper);
        helper.assertTrue(player.startRiding(broom, true), "Safe dismount test player should mount the broom");
        player.setYRot(180.0F);

        var target = broom.getDismountLocationForPassenger(player);
        var minimumSeparation = (broom.getBbWidth() + player.getBbWidth()) / 2.0D;
        helper.assertTrue(target.x - broom.getX() > minimumSeparation,
                "Dismount target should clear both bounding boxes on the broom's left side");
        helper.assertTrue(Math.abs(target.z - broom.getZ()) < 1.0E-5D,
                "Passenger view must not rotate the broom-relative dismount side");
        helper.assertTrue(Math.abs(target.y - (leftGround.getY() + 1.0D)) < 1.0E-5D,
                "Safe dismount should resolve directly to the preferred-side surface");
        helper.assertFalse(broom.isDangerousDismount(),
                "A valid preferred-side surface below two blocks should be safe");

        player.stopRiding();
        helper.assertTrue(Math.abs(player.getY() - (leftGround.getY() + 1.0D)) < 1.0E-5D,
                "Server dismount should place the rider directly on the resolved surface");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void powderSnowRequiresDangerousDismountConfirmation(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 1, 2));
        var leftSurface = center.east();
        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftSurface, Blocks.POWDER_SNOW.defaultBlockState());
        level.setBlockAndUpdate(leftSurface.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftSurface.above(2), Blocks.AIR.defaultBlockState());

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(center.getX() + 0.5D, leftSurface.getY() + 2.5D, center.getZ() + 0.5D);
        broom.setYRot(0.0F);
        level.addFreshEntity(broom);
        var player = serverRider(helper, "powder_snow_dismount_test");
        helper.assertTrue(player.startRiding(broom, true), "Powder snow dismount test player should mount");

        helper.assertTrue(broom.isDangerousDismount(),
                "Powder snow should require dangerous dismount confirmation");
        var target = broom.getDismountLocationForPassenger(player);
        helper.assertTrue(Math.abs(target.y - (leftSurface.getY() + 1.0D)) > 1.0E-5D,
                "Dangerous powder snow must not become an automatic surface dismount target");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dangerousDismountUsesPreferredSideInsteadOfBroomCenter(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 1, 2));
        var leftGround = center.east();
        level.setBlockAndUpdate(center, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(3), Blocks.AIR.defaultBlockState());

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(center.getX() + 0.5D, center.getY() + 2.5D, center.getZ() + 0.5D);
        broom.setYRot(0.0F);
        level.addFreshEntity(broom);
        var player = serverRider(helper);
        helper.assertTrue(player.startRiding(broom, true), "Cliff dismount test player should mount the broom");
        helper.assertTrue(broom.isDangerousDismount(),
                "Ground below the broom must not make a preferred-side cliff safe");

        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.STONE.defaultBlockState());
        helper.assertFalse(broom.isDangerousDismount(),
                "Preferred-side ground should allow a safe dismount even when the broom center is over a cliff");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void safeDismountRequiresSurfaceBelowTwoBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(2, 1, 2));
        var leftGround = center.east();
        level.setBlockAndUpdate(center, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(2), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(leftGround.above(3), Blocks.AIR.defaultBlockState());

        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), level);
        broom.setPos(center.getX() + 0.5D, leftGround.getY() + 3.0D, center.getZ() + 0.5D);
        broom.setYRot(0.0F);
        level.addFreshEntity(broom);
        var player = serverRider(helper);
        helper.assertTrue(player.startRiding(broom, true), "Height boundary test player should mount the broom");

        helper.assertTrue(broom.isDangerousDismount(),
                "A surface exactly two blocks below should require dangerous dismount confirmation");
        var dangerousTarget = broom.getDismountLocationForPassenger(player);
        helper.assertTrue(Math.abs(dangerousTarget.y - broom.getY()) < 1.0E-5D,
                "Dangerous dismount must not warp the rider down to the surface");

        broom.setPos(broom.getX(), leftGround.getY() + 2.99D, broom.getZ());
        helper.assertFalse(broom.isDangerousDismount(),
                "A surface just under two blocks below should allow a direct safe dismount");
        var safeTarget = broom.getDismountLocationForPassenger(player);
        helper.assertTrue(Math.abs(safeTarget.y - (leftGround.getY() + 1.0D)) < 1.0E-5D,
                "Safe dismount below the threshold should resolve to the server surface");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dangerousDismountRequiresReleaseAndSecondPress(GameTestHelper helper) {
        var broom = spawnBroom(helper, 5.0D);
        var player = serverRider(helper);
        helper.assertTrue(player.startRiding(broom, true), "Dismount test player should mount the broom");
        broom.setPos(broom.getX(), broom.getY() + 10.0D, broom.getZ());
        helper.assertTrue(broom.isDangerousDismount(), "High broom should be classified as dangerous");

        BroomDismountEvents.handleSneakInput(player, broom, true);
        var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
        BroomDismountEvents.onDismount(first);
        helper.assertTrue(first.isCanceled(), "First dangerous dismount should be canceled");

        BroomDismountEvents.handleSneakInput(player, broom, true);
        var held = new EntityMountEvent(player, broom, helper.getLevel(), false);
        BroomDismountEvents.onDismount(held);
        helper.assertTrue(held.isCanceled(), "Holding sneak must not confirm dismount");

        BroomDismountEvents.handleSneakInput(player, broom, false);
        helper.assertTrue(player.getVehicle() == broom, "Releasing sneak should not dismount by itself");
        BroomDismountEvents.handleSneakInput(player, broom, true);
        helper.assertFalse(player.isPassenger(), "Second press within thirty ticks should dismount");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void dangerousDismountNeverTreatsHeldSneakAsSecondPress(GameTestHelper helper) {
        var broom = spawnBroom(helper, 5.0D);
        var player = serverRider(helper);
        helper.assertTrue(player.startRiding(broom, true), "Held dismount test player should mount the broom");
        broom.setPos(broom.getX(), broom.getY() + 10.0D, broom.getZ());

        BroomDismountEvents.handleSneakInput(player, broom, true);
        var first = new EntityMountEvent(player, broom, helper.getLevel(), false);
        BroomDismountEvents.onDismount(first);
        helper.assertTrue(first.isCanceled(), "First dangerous dismount should be canceled");

        helper.runAfterDelay(FloatmountBroomEntity.DISMOUNT_CONFIRM_TICKS + 2, () -> {
            BroomDismountEvents.handleSneakInput(player, broom, true);
            var held = new EntityMountEvent(player, broom, helper.getLevel(), false);
            BroomDismountEvents.onDismount(held);
            helper.assertTrue(held.isCanceled(), "Held sneak must remain canceled after the confirmation window");
            helper.assertTrue(player.getVehicle() == broom, "Held sneak must never dismount the rider");
            helper.succeed();
        });
    }

    static FloatmountBroomEntity spawnBroomAt(GameTestHelper helper, BlockPos position) {
        var broom = new FloatmountBroomEntity(EntityRegistry.FLOATMOUNT_BROOM.get(), helper.getLevel());
        broom.setPos(position.getX() + 0.5D, position.getY() + 0.1D, position.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(broom);
        return broom;
    }

    static HoverrideBroomEntity spawnHoverrideBroomAt(GameTestHelper helper, BlockPos position) {
        var broom = new HoverrideBroomEntity(EntityRegistry.HOVERRIDE_BROOM.get(), helper.getLevel());
        broom.setPos(position.getX() + 0.5D, position.getY() + 0.1D, position.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(broom);
        return broom;
    }

    static void prepareVibrationIsolation(GameTestHelper helper) {
        // 前のbatchから落下してきたitemのHIT_GROUND等を遮り、室内の箒由来振動だけを測る。
        for (var position : BlockPos.betweenClosed(-1, 0, -1, 5, 7, 5)) {
            boolean boundary = position.getX() == -1 || position.getX() == 5
                    || position.getY() == 0 || position.getY() == 7
                    || position.getZ() == -1 || position.getZ() == 5;
            helper.setBlock(position, boundary ? Blocks.WHITE_WOOL : Blocks.AIR);
        }
        var interior = new AABB(helper.absolutePos(new BlockPos(0, 1, 0)),
                helper.absolutePos(new BlockPos(5, 7, 5)));
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, interior).forEach(ItemEntity::discard);
    }

    static void installBubbleColumn(ServerLevel level, BlockPos bottom, int height) {
        level.setBlockAndUpdate(bottom.below(), Blocks.SOUL_SAND.defaultBlockState());
        for (var offset = 0; offset < height; ++offset) {
            level.setBlockAndUpdate(
                    bottom.above(offset),
                    Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, false)
            );
        }
    }

    static void installWaterColumn(ServerLevel level, BlockPos bottom, int height) {
        for (var offset = 0; offset < height; ++offset) {
            level.setBlockAndUpdate(bottom.above(offset), Blocks.WATER.defaultBlockState());
        }
    }

    public static Player player(GameTestHelper helper, String name) {
        return serverRider(helper, "broom_" + Integer.toUnsignedString(name.hashCode(), 36));
    }

    public static MagicData magicData(GameTestHelper helper, Player player) {
        var magicData = MagicData.getPlayerMagicData(player);
        helper.assertTrue(magicData != null, "Floatmount Broom test could not resolve player mana data");
        return magicData;
    }
}
