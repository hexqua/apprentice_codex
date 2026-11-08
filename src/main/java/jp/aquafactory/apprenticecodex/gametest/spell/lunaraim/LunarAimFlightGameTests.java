package jp.aquafactory.apprenticecodex.gametest.spell.lunaraim;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.spell.lunaraim.LunarAimFixture.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class LunarAimFlightGameTests {
    private LunarAimFlightGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void straightDelayHomingAndLifetime(GameTestHelper h) {
        try (var s = new LunarAimFixture(h)) {
            var target = s.zombie(12, 0);
            var arrow = s.arrow(target, new Vec3(-1, 0, 0));
            var start = arrow.position();
            for (int i = 0; i < 5; i++) step(arrow);
            h.assertTrue(arrow.position().distanceTo(start.add(-10, 0, 0)) < 1.0e-6, "First five ticks must fly straight at speed two");
            step(arrow);
            double firstTurnDegrees = Math.toDegrees(Math.acos(new Vec3(-1, 0, 0).dot(arrow.getDeltaMovement().normalize())));
            h.assertTrue(Math.abs(firstTurnDegrees - 7.2) < 1.0e-5,
                    "The first homing tick must turn by 7.2 degrees, excluding the straight delay: " + firstTurnDegrees);
            for (int i = 0; i < 100 && !arrow.isBursting(); i++) step(arrow);
            h.assertTrue(arrow.isBursting() && target.getHealth() < 20, "Backwards launch must converge and explode");
            var normal = s.arrow(null, new Vec3(0, 1, 0));
            for (int i = 0; i < 199; i++) {
                normal.setPos(start);
                step(normal);
            }
            h.assertTrue(!normal.isRemoved() && normal.getDeltaMovement().equals(new Vec3(0, 2, 0)), "Unlocked arrows must keep speed and survive until tick 199");
            step(normal);
            h.assertTrue(normal.isRemoved() && !normal.isBursting(), "Tick 200 must silently expire");
            var locked = s.arrow(target, new Vec3(0, 1, 0));
            locked.tickCount = 199;
            step(locked);
            h.assertTrue(locked.isRemoved() && !locked.isBursting(), "Locked arrows must use the same deadline");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void targetLossIsPermanentAndKeepsDeadline(GameTestHelper h) {
        try (var s = new LunarAimFixture(h)) {
            for (var reason : List.of(Entity.RemovalReason.KILLED, Entity.RemovalReason.UNLOADED_TO_CHUNK,
                    Entity.RemovalReason.CHANGED_DIMENSION, Entity.RemovalReason.DISCARDED)) {
                var target = s.zombie(12, 0);
                var arrow = s.arrow(target, new Vec3(0, 0, 1));
                target.remove(reason);
                step(arrow);
                h.assertTrue(!arrow.hasTarget(), "Target removal must clear homing during the straight delay: " + reason);
                var replacement = s.zombie(12, 0, target.getUUID());
                var velocity = arrow.getDeltaMovement();
                for (int i = 0; i < 198; i++) {
                    arrow.setPos(s.origin.add(0, 8, 0));
                    step(arrow);
                }
                h.assertTrue(!arrow.isRemoved() && !arrow.hasTarget() && arrow.getDeltaMovement().equals(velocity), "Target loss must preserve straight flight and the original deadline");
                step(arrow);
                h.assertTrue(arrow.isRemoved() && !arrow.isBursting(), "Lost-target arrows must silently expire at 200 ticks");
                replacement.discard();
            }
            var target = s.zombie(15, 0);
            var arrow = s.arrow(target, new Vec3(0, 1, 0));
            for (int i = 0; i < 11; i++) step(arrow);
            target.setHealth(0);
            step(arrow);
            var velocity = arrow.getDeltaMovement();
            step(arrow);
            h.assertTrue(!arrow.hasTarget() && velocity.equals(arrow.getDeltaMovement()), "Death after homing begins must freeze the last velocity");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 80)
    public static void realServerTicksPreserveDelayAndLoseTargets(GameTestHelper h) {
        var s = new LunarAimFixture(h);
        var target = s.zombie(12, 0);
        // 実tickの移動と手動stepの双方で、tickCountの数え方が一致することを確認する。
        var arrow = s.arrow(target, new Vec3(0, 1, 0));
        h.runAfterDelay(3, () -> {
            try {
                h.assertTrue(arrow.tickCount > 0 && arrow.tickCount <= 5 && arrow.getDeltaMovement().equals(new Vec3(0, 2, 0)),
                        "Server ticks must retain straight movement during the initial delay");
            } catch (Throwable failure) {
                s.close();
                throw failure;
            }
        });
        h.runAfterDelay(8, () -> {
            try {
                h.assertTrue(arrow.hasTarget() && arrow.getDeltaMovement().x > 0, "Server ticks must begin steering after the delay");
                target.discard();
            } catch (Throwable failure) {
                s.close();
                throw failure;
            }
        });
        h.runAfterDelay(11, () -> {
            try (s) {
                h.assertTrue(!arrow.isRemoved() && !arrow.isBursting() && !arrow.hasTarget(), "Target removal must leave a live straight arrow");
            }
            h.succeed();
        });
    }
}
