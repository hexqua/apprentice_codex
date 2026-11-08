package jp.aquafactory.apprenticecodex.gametest.equipment.broom.movement;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.entity.broom.AbstractBroomEntity;
import jp.aquafactory.apprenticecodex.entity.broom.HoverrideBroomMovement;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.serverRider;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.spawnBroom;
import static jp.aquafactory.apprenticecodex.gametest.equipment.broom.support.BroomTestSupport.spawnHoverrideBroom;

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

        // 最低速度・上限は入力で与え、現在の既定の最高速度を転記して期待値にしない。
        var released = HoverrideBroomMovement.releaseHorizontal(Vec3.ZERO, target, 0.23D, 0.73D);
        helper.assertTrue(released.distanceTo(new Vec3(0, 0, 0.23D)) < 1.0e-6D,
                "Inertia release must use the supplied minimum speed and forward direction");
        var capped = HoverrideBroomMovement.releaseHorizontal(new Vec3(4, 0, 0), target, 0.23D, 0.73D);
        helper.assertTrue(capped.distanceTo(new Vec3(0, 0, 0.73D)) < 1.0e-6D,
                "Inertia release must cap existing momentum at the supplied maximum speed");
        var normalAcceleration = HoverrideBroomMovement.normalHorizontal(Vec3.ZERO, target, 1.0F, true);
        var overdriveAcceleration = HoverrideBroomMovement.normalHorizontal(Vec3.ZERO, target, 1.0F, true, true);
        helper.assertTrue(overdriveAcceleration.length() > normalAcceleration.length()
                        && overdriveAcceleration.normalize().distanceTo(target) < 1.0e-6D,
                "Overdrive must increase acceleration without changing its direction");
        var initial = new Vec3(0.4D, 0, 0);
        var normalBraking = HoverrideBroomMovement.normalHorizontal(initial, Vec3.ZERO, -1.0F, true);
        var overdriveBraking = HoverrideBroomMovement.normalHorizontal(initial, Vec3.ZERO, -1.0F, true, true);
        helper.assertTrue(overdriveBraking.x >= 0 && overdriveBraking.length() < normalBraking.length(),
                "Overdrive braking must slow the broom more strongly without reversing");
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

    private static void assertDismountPreservesMovement(
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
