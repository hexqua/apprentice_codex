package jp.aquafactory.apprenticecodex.gametest.equipment.mantle;

import io.redspace.ironsspellbooks.registries.ItemRegistry;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleBlink;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.MantleMovement;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantle;
import jp.aquafactory.apprenticecodex.item.curios.shootingstarmantle.ShootingStarMantleRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static jp.aquafactory.apprenticecodex.gametest.equipment.mantle.ShootingStarMantleForgeGameTests.TEMPLATE;
import static jp.aquafactory.apprenticecodex.gametest.equipment.mantle.ShootingStarMantleForgeGameTests.equippedPlayer;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class MantleBlinkGameTests {
    private MantleBlinkGameTests() {}

    @GameTest(template = TEMPLATE)
    public static void directHeightChangeKeepsRemainingMantleBlinkMovement(GameTestHelper helper) {
        var player = equippedPlayer(helper, "mantle_blink_height_change");
        var stack = ShootingStarMantleRuntime.findEquipped(player);
        ((ShootingStarMantle) stack.getItem()).trySetCalibrationAdjustment(stack, 0,
                new ItemStack(ItemRegistry.ENDER_RUNE.get()));
        for (int x = 0; x <= 7; x++) for (int y = 1; y <= 5; y++) {
            helper.setBlock(new BlockPos(x, y, 0), Blocks.AIR);
        }
        var origin = helper.absoluteVec(new Vec3(0.5, 2, 0.5));
        player.setPos(origin);
        player.setYRot(-90);
        for (int tick = 0; tick < MantleBlink.DURATION; tick++) {
            int age = tick;
            helper.runAtTickTime(tick + 1, () -> {
                if (age == 0) {
                    helper.assertTrue(ShootingStarMantleRuntime.toggle(player), "Mantle must enter hover before blinking");
                    helper.assertTrue(ShootingStarMantleRuntime.impulse(player, 0, 1, 0), "Ender rune must start blink");
                }
                if (age == 1) player.setPos(player.getX(), player.getY() + 1, player.getZ());
                var state = ShootingStarMantleRuntime.state(player);
                ShootingStarMantleRuntime.tick(player);
                MantleMovement.travel(player, Vec3.ZERO, state);
                double expected = Math.min(4, Math.max(0, age - 2)) * 1.5;
                helper.assertTrue(Math.abs(player.position().subtract(origin).horizontalDistance() - expected) < 1.0e-5,
                        "Direct height changes must not shorten mantle blink's horizontal travel");
                helper.assertTrue(Math.abs(player.getY() - origin.y - Math.min(age, 1)) < 1.0e-6,
                        "Mantle blink must keep the externally assigned height");
                helper.assertTrue(state.blink.active(player.level().getGameTime()),
                        "Direct height changes must not cancel mantle blink");
            });
        }
        helper.runAtTickTime(11, () -> {
            ShootingStarMantleRuntime.clear(player);
            helper.succeed();
        });
    }
}
