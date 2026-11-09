package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.sacred;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.sacred.SacredArrowTargetingGameTests.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class SacredArrowFlightGameTests {
    private SacredArrowFlightGameTests() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void lostTargetContinuesStraightAndExpires(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(15, 0);
            var arrow = s.arrow(target, new Vec3(1, 0, 0)); target.discard(); step(arrow);
            var velocity = arrow.getDeltaMovement();
            var other = s.zombie(3, 0);
            step(arrow);
            h.assertTrue(other.getHealth() == 18 && !arrow.isRemoved(), "Lost-target arrows must retain reduced piercing damage");
            for (int i = 0; i < 58; i++) { arrow.setPos(s.origin.add(0, 10, 0)); step(arrow); }
            h.assertTrue(!arrow.isRemoved() && arrow.getDeltaMovement().distanceTo(velocity) < 1.0e-6, "Lost-target arrows must fly straight without gravity");
            step(arrow); h.assertTrue(arrow.isRemoved(), "Lost-target lifetime must be capped at 60 ticks");
        }
        h.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void reverseHomingConvergesAndNormalFlightFalls(GameTestHelper h) {
        try (var s = new Scene(h)) {
            var target = s.zombie(12, 0);
            var homing = s.arrow(target, new Vec3(-1, 0, 0));
            for (int i = 0; i < 100 && !homing.isRemoved(); i++) step(homing);
            h.assertTrue(homing.isRemoved() && target.getHealth() < 20, "Even backwards launch must converge onto the target");
            target.discard();
            var arrow = s.arrow(null, new Vec3(0, 0, 1)); var start = arrow.position();
            for (int i = 0; i < 10; i++) step(arrow);
            h.assertTrue(arrow.position().distanceTo(start.add(0, 0, 20)) < 1.0e-6, "First ten movements must travel at two blocks per tick without gravity");
            step(arrow);
            h.assertTrue(Math.abs(arrow.getDeltaMovement().y + 0.05) < 1.0e-6, "Gravity must start after ten complete ticks");
            arrow.tickCount = 199; step(arrow);
            h.assertTrue(arrow.isRemoved(), "All arrows must expire at 200 ticks");
        }
        h.succeed();
    }
}
