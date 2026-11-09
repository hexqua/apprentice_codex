package jp.aquafactory.apprenticecodex.gametest.crafting;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

final class ComfortBerriesScenarios {
    private ComfortBerriesScenarios() {}

    static void comfortBerriesCanBePottedAsDecoration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pottedComfortBerryBush = (FlowerPotBlock) BlockRegistry.POTTED_COMFORT_BERRY_BUSH.get();
            helper.assertTrue(pottedComfortBerryBush.getContent() == BlockRegistry.COMFORT_BERRY_BUSH.get(),
                    "Potted Comfort Berry Bush should contain the Comfort Berry Bush block");
            helper.assertTrue(pottedComfortBerryBush.defaultBlockState().getLightEmission(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))) == 10,
                    "Potted Comfort Berry Bush should emit light level 10");

            var potPos = new BlockPos(1, 1, 1);
            var absolutePotPos = helper.absolutePos(potPos);
            helper.setBlock(potPos, Blocks.FLOWER_POT);

            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "comfort_berry_pot_test"));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.COMFORT_BERRIES.get()));
            var result = helper.getLevel().getBlockState(absolutePotPos).use(
                    helper.getLevel(),
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(absolutePotPos), Direction.UP, absolutePotPos, false)
            );

            helper.assertTrue(result.consumesAction(), "Comfort Berries should be accepted by a vanilla Flower Pot");
            helper.assertBlockPresent(BlockRegistry.POTTED_COMFORT_BERRY_BUSH.get(), potPos);
            helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                    "Potted Comfort Berries should consume one berry item outside creative mode");
        });
    }

    static void comfortBerriesHaveAppleTierCompostChance(GameTestHelper helper) {
        helper.succeedIf(() -> helper.assertTrue(Float.compare(
                        ComposterBlock.COMPOSTABLES.getFloat(ItemRegistry.COMFORT_BERRIES.get()),
                        0.65F
                ) == 0,
                "Comfort Berries should have the apple-tier compost chance of 0.65"));
    }
}
