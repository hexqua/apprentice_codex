package jp.aquafactory.apprenticecodex.gametest.crafting;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

final class ComfortBerriesScenarios {
    private ComfortBerriesScenarios() {}

    static void comfortBerriesCanBePottedAsDecoration(GameTestHelper helper) {
        helper.succeedIf(() -> {
            var pottedComfortBerryBush = (FlowerPotBlock) BlockRegistry.POTTED_COMFORT_BERRY_BUSH.get();
            helper.assertTrue(pottedComfortBerryBush.getPotted() == BlockRegistry.COMFORT_BERRY_BUSH.get(),
                    "Potted Comfort Berry Bush should contain the Comfort Berry Bush block");
            helper.assertTrue(pottedComfortBerryBush.defaultBlockState().getLightEmission(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1))) == 10,
                    "Potted Comfort Berry Bush should emit light level 10");

            var potPos = new BlockPos(1, 1, 1);
            var absolutePotPos = helper.absolutePos(potPos);
            helper.setBlock(potPos, Blocks.FLOWER_POT);

            var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "comfort_berry_pot_test"));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemRegistry.COMFORT_BERRIES.get()));
            var result = player.gameMode.useItemOn(
                    player,
                    helper.getLevel(),
                    player.getItemInHand(InteractionHand.MAIN_HAND),
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
        helper.succeedIf(() -> {
            // 1.20.1 へ backport する際はこのデータマップを移植せず、Forge の ComposterBlock 登録で
            // 同じ 0.65F と農民不可を再実装し、この契約テストも登録値を読む形へ置き換える。
            Compostable compostable = ItemRegistry.COMFORT_BERRIES.get().builtInRegistryHolder()
                    .getData(NeoForgeDataMaps.COMPOSTABLES);
            helper.assertTrue(compostable != null, "Comfort Berries should be registered as compostable");
            if (compostable == null) {
                return;
            }

            helper.assertTrue(Float.compare(compostable.chance(), 0.65F) == 0,
                    "Comfort Berries should have the apple-tier compost chance of 0.65");
            helper.assertFalse(compostable.canVillagerCompost(),
                    "Farmer villagers should not compost Comfort Berries");
        });
    }
}
