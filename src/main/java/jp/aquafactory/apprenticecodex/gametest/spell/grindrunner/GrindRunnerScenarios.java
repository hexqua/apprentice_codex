package jp.aquafactory.apprenticecodex.gametest.spell.grindrunner;

import jp.aquafactory.apprenticecodex.registry.EntityRegistry;
import jp.aquafactory.apprenticecodex.spell.grindrunner.GrindRunnerWheelEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;

import java.util.function.Predicate;

import static jp.aquafactory.apprenticecodex.gametest.integration.create.CreateProcessingTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.support.EntityTestSupport.spawnNoGravityItem;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createEquipmentTestPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.RegistryTestSupport.requireForgeItem;

final class GrindRunnerScenarios {
    private GrindRunnerScenarios() {}

    static void grindRunnerProcessesCreateCrushingWithoutCraftsmansDelight(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var harness = startSingleGrindRunnerItemProcess(helper, new ItemStack(Items.AMETHYST_CLUSTER));
        helper.runAtTickTime(12, () -> assertProcessedGrindRunnerOutput(
                helper,
                harness,
                output -> output.is(Items.AMETHYST_SHARD),
                "Grind Runner should process Create crushing recipes without Craftsman's Delight"
        ));
    }

    static void grindRunnerProcessesCreateMillingRecipes(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var wheatFlour = requireForgeItem(helper, ResourceLocation.fromNamespaceAndPath(CREATE_MOD_ID, "wheat_flour"));
        var harness = startSingleGrindRunnerItemProcess(helper, new ItemStack(Items.WHEAT));
        helper.runAtTickTime(12, () -> assertProcessedGrindRunnerOutput(
                helper,
                harness,
                output -> output.is(wheatFlour),
                "Grind Runner should process Create milling recipes after crushing misses"
        ));
    }

    static void grindRunnerPrefersCreateCrushingBeforeMilling(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var harness = startSingleGrindRunnerItemProcess(helper, new ItemStack(Items.GRAVEL));
        helper.runAtTickTime(12, () -> assertProcessedGrindRunnerOutput(
                helper,
                harness,
                output -> output.is(Items.SAND),
                "Grind Runner should prefer Create crushing over Create milling for overlapping inputs"
        ));
    }

    static void grindRunnerProcessesCreateDepotItems(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var targetPos = new BlockPos(2, 1, 0);
        var harness = startGrindRunnerCreateBlockProcess(helper, targetPos);
        invokeCreateGameTestHookVoid(
                "placeDepotWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                helper.getLevel(),
                helper.absolutePos(targetPos),
                new ItemStack(Items.GRAVEL)
        );

        helper.runAtTickTime(12, () -> {
            try {
                var result = invokeCreateGameTestHookItemStack("getDepotItem", helper.getLevel(), helper.absolutePos(targetPos));
                helper.assertTrue(result.is(Items.SAND), "Grind Runner should process Create Depot items: " + result);
            } finally {
                harness.discard();
            }
            helper.succeed();
        });
    }

    static void grindRunnerLeavesCreateChuteItemsUnprocessed(GameTestHelper helper) {
        if (!ModList.get().isLoaded(CREATE_MOD_ID)) {
            helper.succeed();
            return;
        }

        var targetPos = new BlockPos(2, 1, 0);
        helper.setBlock(targetPos.below(), Blocks.STONE);
        var harness = startGrindRunnerCreateBlockProcess(helper, targetPos);
        invokeCreateGameTestHookVoid(
                "placeChuteWithItem",
                new Class<?>[]{ServerLevel.class, BlockPos.class, ItemStack.class},
                helper.getLevel(),
                helper.absolutePos(targetPos),
                new ItemStack(Items.GRAVEL)
        );

        helper.runAtTickTime(12, () -> {
            try {
                var result = invokeCreateGameTestHookItemStack("getChuteItem", helper.getLevel(), helper.absolutePos(targetPos));
                helper.assertTrue(result.is(Items.GRAVEL), "Grind Runner should not process enclosed Create Chute items: " + result);
            } finally {
                harness.discard();
            }
            helper.succeed();
        });
    }

    static GrindRunnerProcessHarness startSingleGrindRunnerItemProcess(GameTestHelper helper, ItemStack inputStack) {
        var owner = createEquipmentTestPlayer(helper, new BlockPos(2, 2, 0), "grind_runner_processing_test");
        helper.getLevel().addFreshEntity(owner);
        var wheel = new GrindRunnerWheelEntity(EntityRegistry.GRIND_RUNNER_WHEEL.get(), helper.getLevel(), owner);
        wheel.setGrindItemPerSecond(20.0F);
        helper.getLevel().addFreshEntity(wheel);

        var itemEntity = spawnNoGravityItem(helper, new BlockPos(2, 2, 0), inputStack.copyWithCount(1));
        return new GrindRunnerProcessHarness(owner, wheel, itemEntity);
    }

    static GrindRunnerCreateBlockHarness startGrindRunnerCreateBlockProcess(GameTestHelper helper, BlockPos targetPos) {
        var owner = createEquipmentTestPlayer(helper, targetPos.above(), "grind_runner_create_block_processing_test");
        helper.getLevel().addFreshEntity(owner);
        var wheel = new GrindRunnerWheelEntity(EntityRegistry.GRIND_RUNNER_WHEEL.get(), helper.getLevel(), owner);
        wheel.setGrindItemPerSecond(20.0F);
        helper.getLevel().addFreshEntity(wheel);
        return new GrindRunnerCreateBlockHarness(owner, wheel);
    }

    static void assertProcessedGrindRunnerOutput(
            GameTestHelper helper,
            GrindRunnerProcessHarness harness,
            Predicate<ItemStack> outputPredicate,
            String message
    ) {
        var result = harness.itemEntity().isAlive() ? harness.itemEntity().getItem().copy() : ItemStack.EMPTY;
        try {
            helper.assertTrue(outputPredicate.test(result), message + ": " + result);
        } finally {
            harness.discard();
        }
        helper.succeed();
    }

    private record GrindRunnerProcessHarness(FakePlayer owner, GrindRunnerWheelEntity wheel, ItemEntity itemEntity) {
        private void discard() {
            itemEntity.discard();
            wheel.discard();
            owner.discard();
        }
    }

    private record GrindRunnerCreateBlockHarness(FakePlayer owner, GrindRunnerWheelEntity wheel) {
        private void discard() {
            wheel.discard();
            owner.discard();
        }
    }
}
