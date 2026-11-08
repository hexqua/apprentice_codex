package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.bloody;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.function.Consumer;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.bloody.BloodyArrowOrbsGameTests.*;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class BloodyArrowFlightGameTests {
    private BloodyArrowFlightGameTests() {}

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void flightDelaysGravityAndExpires(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var origin = helper.absoluteVec(new Vec3(2, 400, 2));
            var arrow = scene.arrow(origin, new Vec3(1, 0, 0), 1, 3);
            for (int i = 0; i < 10; i++) step(arrow);
            helper.assertTrue(arrow.position().distanceTo(origin.add(25, 0, 0)) < 1.0e-6,
                    "Arrow must travel 25 blocks without falling in its first ten ticks");
            step(arrow);
            helper.assertTrue(Math.abs(arrow.getY() - origin.y) < 1.0e-6
                            && Math.abs(arrow.getDeltaMovement().y + 0.05) < 1.0e-6,
                    "Gravity must begin after ten complete ticks without gravity");
            step(arrow);
            helper.assertTrue(Math.abs(arrow.getY() - origin.y + 0.05) < 1.0e-6,
                    "The next movement must include the first gravity step");
            // 寿命だけを検証する区間では地形や未読込チャンクへ飛ばさない。
            arrow.setPos(origin);
            arrow.setDeltaMovement(Vec3.ZERO);
            arrow.setNoGravity(true);
            for (int i = arrow.tickCount; i < 199; i++) step(arrow);
            helper.assertFalse(arrow.isRemoved(), "Arrow must remain until its lifetime boundary");
            step(arrow);
            helper.assertTrue(arrow.isRemoved() && !arrow.shouldBeSaved(), "Arrow must expire at 200 ticks and never save");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void waterFlowAndLavaDoNotChangeFlight(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var origin = helper.absoluteVec(new Vec3(1.5, 240.3, 2.5));
            for (var state : List.of(Blocks.AIR.defaultBlockState(), Blocks.WATER.defaultBlockState(),
                    Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3), Blocks.LAVA.defaultBlockState())) {
                // 水と溶岩を逐次置換すると隣接更新で石になる。各条件を空の区間から始める。
                for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, 240, 2), Blocks.AIR);
                for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, 240, 2), state);
                var arrow = scene.arrow(origin, new Vec3(1, 0, 0), 1, 3);
                step(arrow);
                helper.assertTrue(arrow.position().distanceTo(origin.add(2.5, 0, 0)) < 1.0e-6
                                && arrow.getDeltaMovement().distanceTo(new Vec3(2.5, 0, 0)) < 1.0e-6,
                        "Fluid motion mismatch: state=" + state + ", position=" + arrow.position()
                                + ", velocity=" + arrow.getDeltaMovement() + ", removed=" + arrow.isRemoved());
                arrow.discard();
            }
            for (int x = 1; x <= 3; x++) helper.setBlock(new BlockPos(x, 240, 2), Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void terrainStopsFastShotsBeforeTargets(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            helper.setBlock(new BlockPos(2, 240, 3), Blocks.STONE);
            var target = scene.zombie(new Vec3(3.5, 240, 3.5));
            for (double x : List.of(1.99, 2.5)) {
                var arrow = scene.arrow(helper.absoluteVec(new Vec3(x, 240.5, 3.5)), new Vec3(1, 0, 0), 10, 3);
                step(arrow);
                helper.assertTrue(arrow.isRemoved() && target.getHealth() == 20 && scene.orbs().isEmpty(),
                        "Near-wall and inside-wall shots must not hit targets behind terrain");
            }
            helper.setBlock(new BlockPos(2, 240, 3), Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = BATCH)
    public static void cancelledImpactContinuesWithoutDamage(GameTestHelper helper) {
        try (var scene = new Scene(helper)) {
            var target = scene.zombie(new Vec3(3, 240, 3));
            target.setBaby(true);
            var arrow = scene.arrow(helper.absoluteVec(new Vec3(0.8, 240.4, 3)), new Vec3(1, 0, 0), 10, 3);
            Consumer<ProjectileImpactEvent> listener = event -> {
                if (event.getProjectile() == arrow) event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            };
            MinecraftForge.EVENT_BUS.addListener(listener);
            try {
                step(arrow);
                helper.assertTrue(!arrow.isRemoved() && target.getHealth() == 20 && scene.orbs().isEmpty(),
                        "Cancelled impacts must not damage or create orbs");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(listener);
            }
            arrow.discard();
            var hit = scene.arrow(helper.absoluteVec(new Vec3(0.8, 240.4, 3)), new Vec3(1, 0, 0), 2, 3);
            step(hit);
            helper.assertTrue(hit.isRemoved() && target.getHealth() < 20 && scene.orbs().size() == 3,
                    "An uncancelled fast arrow must hit a small target between tick positions");
        }
        helper.succeed();
    }
}
