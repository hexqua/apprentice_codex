package jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.echo;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.echoarrow.EchoArrowCoreEntity;
import jp.aquafactory.apprenticecodex.spell.echoarrow.EchoArrowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.bow.arrows.echo.EchoArrowFixture.step;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class EchoArrowVolleyGameTests {
    private EchoArrowVolleyGameTests() {
    }

    private static final String TEMPLATE = "gametest/echo_arrow_flight";
    private static final String BATCH = "apprenticecodex.echo_arrow";

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 60)
    public static void actualTicksFireExactBatchesAtFixedImpactPoint(GameTestHelper helper) {
        var scene = new EchoArrowFixture(helper);
        var target = scene.zombie(new Vec3(3, 30, 3));
        target.setInvulnerable(true);
        var pair = scene.pair(new Vec3(.8, 31, 3), new Vec3(1, 0, 0), 7);
        step(pair.arrow());
        Vec3 impact = pair.arrow().position();
        long impactTime = helper.getLevel().getGameTime();
        helper.assertTrue(pair.arrow().isRemoved() && !pair.core().isRemoved(), "Rejected damage must still arm the core");
        helper.assertTrue(impact.y > target.getY() + .5, "Aim must use the projectile impact, not target feet");
        target.setPos(target.position().add(0, 5, 0));
        pair.core().acceptImpact(pair.arrow(), impact.add(0, 10, 0));
        helper.runAfterDelay(20, () -> {
            try (scene) {
                var shots = scene.shots.stream().filter(s -> s.arrow() != pair.arrow()).toList();
                helper.assertTrue(shots.size() == 7, "Seven follow-up arrows must exclude the initial arrow: " + shots.size());
                for (int i = 0; i < shots.size(); i++) {
                    var shot = shots.get(i);
                    helper.assertTrue(shot.time() == impactTime + 10 + i,
                            "Follow-up arrows must fire individually on consecutive server ticks: " + shot.time() + ", impact=" + impactTime);
                    helper.assertTrue(shot.velocity().length() >= 2.4 && shot.velocity().length() <= 2.6,
                            "Follow-up speed must stay in the configured range");
                    helper.assertTrue(shot.velocity().normalize().distanceTo(impact.subtract(shot.origin()).normalize()) < 1e-6,
                            "Every arrow must converge on the original impact after target movement");
                    helper.assertTrue(shot.arrow().isNoGravity(), "Follow-up arrows must remain gravity-free");
                }
                helper.assertTrue(shots.get(0).origin().distanceTo(shots.get(1).origin()) > .1,
                        "Simultaneous arrows must have distinct spawn positions");
                helper.assertTrue(pair.core().isRemoved(), "Core must disappear after the final arrow");
                helper.assertTrue(shots.stream().anyMatch(s -> !s.arrow().isRemoved()), "Core completion must preserve fired arrows");
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 60)
    public static void counteringDuringVolleyStopsOnlyUnfiredArrows(GameTestHelper helper) {
        var scene = new EchoArrowFixture(helper);
        scene.zombie(new Vec3(3, 30, 3));
        var pair = scene.pair(new Vec3(.8, 31, 3), new Vec3(1, 0, 0), 15);
        step(pair.arrow());
        int[] emittedBeforeCounter = {0};
        helper.runAfterDelay(13, () -> {
            // 毎tick発射ではGameTest callbackとentity tickの実行順で境界本数が変わる。
            // 正確な発射時刻は別テストで確認し、ここでは途中停止後に増えないことを検証する。
            emittedBeforeCounter[0] = scene.shots.size();
            helper.assertTrue(emittedBeforeCounter[0] > 1 && emittedBeforeCounter[0] < 16,
                    "Counterspell must be applied after firing begins and before all arrows are emitted");
            pair.core().onAntiMagic(null);
        });
        helper.runAfterDelay(25, () -> {
            try (scene) {
                helper.assertTrue(scene.shots.size() == emittedBeforeCounter[0] && pair.core().isRemoved(),
                        "Counterspell must stop all remaining volleys");
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 60)
    public static void zeroCountAndMissedDeadlineCannotLeaveCoreActive(GameTestHelper helper) {
        var scene = new EchoArrowFixture(helper);
        var zero = scene.pair(new Vec3(2, 60, 2), new Vec3(0, 1, 0), 0);
        zero.core().acceptImpact(zero.arrow(), zero.arrow().position().add(0, 10, 0));
        helper.assertTrue(zero.core().isRemoved(), "Zero follow-up count must finish immediately at impact");
        // worldへ登録しない核でtick停止を再現し、遅れて再開しても発射しないことを確認する。
        var arrow = new EchoArrowEntity(EntityRegistry.ECHO_ARROW.get(), helper.getLevel());
        var core = new EchoArrowCoreEntity(EntityRegistry.ECHO_ARROW_CORE.get(), helper.getLevel());
        core.configure(scene.owner, scene.pos(new Vec3(2, 60, 2)), 3, 3, arrow);
        core.acceptImpact(arrow, scene.pos(new Vec3(2, 70, 2)));
        helper.runAfterDelay(12, () -> {
            try (scene) {
                core.tick();
                helper.assertTrue(core.isRemoved() && scene.shots.size() == 1,
                        "Expired echoes must not resume or catch up missed volleys");
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, batch = BATCH, timeoutTicks = 60)
    public static void blockedCoreConsumesArrowsWithoutSpawningAcrossWall(GameTestHelper helper) {
        var scene = new EchoArrowFixture(helper);
        scene.zombie(new Vec3(3, 30, 3));
        var pair = scene.pair(new Vec3(.98, 31, 3), new Vec3(1, 0, 0), 5);
        step(pair.arrow());
        var wall = new BlockPos(0, 31, 3);
        scene.setBlock(wall, Blocks.STONE);
        helper.runAfterDelay(17, () -> {
            try (scene) {
                helper.assertTrue(pair.core().isRemoved() && scene.shots.size() == 1,
                        "A blocked core must consume all scheduled slots without spawning arrows beyond the wall");
            } finally {
                scene.setBlock(wall, Blocks.AIR);
            }
            helper.succeed();
        });
    }
}
