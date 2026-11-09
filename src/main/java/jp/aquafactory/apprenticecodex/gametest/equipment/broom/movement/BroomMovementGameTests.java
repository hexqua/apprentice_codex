package jp.aquafactory.apprenticecodex.gametest.equipment.broom.movement;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomMovement;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.environment.BroomEnvironmentGameTests.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BroomMovementGameTests {
    private BroomMovementGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void hoverrideMovementMathMatchesPrototypeContract(GameTestHelper helper) {
        var movement = new Vec3(HoverrideBroomMovement.MAX_HORIZONTAL_SPEED, 0.0D, 0.0D);
        for (var tick = 0; tick < 41; tick++) {
            movement = HoverrideBroomMovement.normalHorizontal(movement, new Vec3(0.0D, 0.0D, 1.0D),
                    0.0F, true);
        }
        helper.assertTrue(movement.lengthSqr() == 0.0D,
                "Hoverride Broom should reach its practical stop speed in about two seconds");

        movement = new Vec3(HoverrideBroomMovement.MAX_HORIZONTAL_SPEED, 0.0D, 0.0D);
        for (var tick = 0; tick < 10; tick++) {
            movement = HoverrideBroomMovement.normalHorizontal(movement, Vec3.ZERO, -1.0F, true);
        }
        helper.assertTrue(movement.lengthSqr() < 1.0e-8D,
                "Hoverride Broom braking must stop without reversing");

        var direction = new Vec3(1.0D, 0.0D, 0.0D);
        var target = new Vec3(0.0D, 0.0D, 1.0D);
        for (var tick = 0; tick < 6; tick++) {
            direction = HoverrideBroomMovement.rotateToward(direction, target, 0.4D);
        }
        helper.assertTrue(direction.dot(target) > 0.995D,
                "Hoverride Broom velocity should substantially follow its yaw within six ticks");

        var released = HoverrideBroomMovement.releaseHorizontal(
                Vec3.ZERO,
                target,
                HoverrideBroomMovement.MAX_HORIZONTAL_SPEED * 0.5D
        );
        helper.assertTrue(Math.abs(released.length() - 0.275D) < 1.0e-6D,
                "Hoverride Broom release should restore the configured minimum speed");

        var overdriveMaximumSpeed = HoverrideBroomMovement.maximumHorizontalSpeed(true);
        helper.assertTrue(Math.abs(overdriveMaximumSpeed - 0.825D) < 1.0e-9D,
                "Overdrive should increase Hoverride maximum horizontal speed by fifty percent");
        var overdriveAcceleration = HoverrideBroomMovement.normalHorizontal(
                Vec3.ZERO, target, 1.0F, true, true
        );
        helper.assertTrue(Math.abs(overdriveAcceleration.length() - 0.06D) < 1.0e-9D,
                "Overdrive should increase Hoverride horizontal acceleration by fifty percent");
        var overdriveBraking = HoverrideBroomMovement.normalHorizontal(
                new Vec3(overdriveMaximumSpeed, 0.0D, 0.0D), Vec3.ZERO, -1.0F, true, true
        );
        helper.assertTrue(Math.abs(overdriveBraking.length() - 0.7425D) < 1.0e-9D,
                "Overdrive should increase Hoverride braking by fifty percent");
        var overdriveReleased = HoverrideBroomMovement.releaseHorizontal(
                Vec3.ZERO, target, overdriveMaximumSpeed * 0.8D, overdriveMaximumSpeed
        );
        helper.assertTrue(Math.abs(overdriveReleased.length() - 0.66D) < 1.0e-9D,
                "Overdrive release should restore eighty percent of its maximum speed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void dismountPreservesServerObservedBroomMovement(GameTestHelper helper) {
        assertDismountPreservesMovement(helper, spawnBroom(helper, 1.5D), "Floatmount");
        assertDismountPreservesMovement(helper, spawnHoverrideBroom(helper, 1.5D), "Hoverride");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hoverridePassengerStandsAndSmoothlyFollowsBroomYaw(GameTestHelper helper) {
        var floatmount = spawnBroom(helper, 1.5D);
        var hoverride = spawnHoverrideBroom(helper, 1.5D);
        helper.assertTrue(floatmount.shouldRiderSit(), "Floatmount Broom should retain its seated rider pose");
        helper.assertFalse(hoverride.shouldRiderSit(), "Hoverride Broom should render its rider standing");

        var floatmountPlayer = serverRider(helper);
        helper.assertTrue(floatmountPlayer.startRiding(floatmount, true), "Floatmount pose reference rider should mount");
        floatmount.positionRider(floatmountPlayer);
        var player = serverRider(helper);
        player.setYRot(0.0F);
        helper.assertTrue(player.startRiding(hoverride, true), "Hoverride yaw-follow rider should mount");
        hoverride.setYRot(0.0F);
        hoverride.positionRider(player);
        helper.assertTrue(Math.abs(player.getY() - floatmountPlayer.getY() - 0.625D) < 1.0e-6D,
                "Hoverride standing rider should receive its dedicated vertical position correction");

        hoverride.setYRot(60.0F);
        hoverride.positionRider(player);
        helper.assertTrue(player.getYRot() > 20.0F && player.getYRot() < 30.0F,
                "Hoverride yaw follow should begin smoothly instead of snapping");
        for (var tick = 1; tick < 6; tick++) {
            hoverride.positionRider(player);
        }
        helper.assertTrue(Math.abs(Mth.wrapDegrees(player.getYRot() - 60.0F)) < 4.0F,
                "Hoverride rider view should substantially follow a turn within six ticks");
        helper.assertTrue(Math.abs(Mth.wrapDegrees(player.getYHeadRot() - player.getYRot())) < 1.0e-4F,
                "Hoverride rider head should follow the adjusted camera yaw");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unoccupiedBroomUsesCoastDamping(GameTestHelper helper) {
        var broom = spawnBroom(helper, 1.5D);
        broom.setDeltaMovement(0.2D, 0.0D, 0.0D);

        broom.tick();

        helper.assertTrue(Math.abs(broom.getDeltaMovement().x - 0.17D) < 1.0e-6D,
                "Unoccupied broom should retain eighty-five percent of horizontal speed per tick");
        helper.succeed();
    }

    static void assertDismountPreservesMovement(
            GameTestHelper helper,
            AbstractBroomEntity broom,
            String broomName
    ) {
        var player = serverRider(helper);
        player.getAbilities().instabuild = true;
        helper.assertTrue(player.startRiding(broom, true), broomName + " rider should mount");

        broom.tick();
        broom.setPos(broom.getX() + 0.2D, broom.getY(), broom.getZ());
        broom.tick();
        var dismountX = broom.getX();
        player.stopRiding();

        helper.assertTrue(broom.getDeltaMovement().x > 0.19D,
                broomName + " should inherit its server-observed movement on dismount");
        broom.tick();
        helper.assertTrue(broom.getX() > dismountX + 0.15D,
                broomName + " should continue coasting after dismount");
        broom.discard();
    }
}
