package jp.aquafactory.apprenticecodex.gametest;

import com.mojang.authlib.GameProfile;
import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJar;
import jp.aquafactory.apprenticecodex.block.arcanuminajar.ArcanumInAJarBlockEntity;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.registry.BlockRegistry;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.UUID;

@GameTestHolder(ApprenticeCodex.MODID)
@PrefixGameTestTemplate(false)
public final class ArcanumInAJarGameTests {
    private static final String TEMPLATE = "gametest/basic_floor";
    private static final String CONFIG_BATCH = "arcanum_in_a_jar_config";
    private static final BlockPos JAR_POS = new BlockPos(1, 1, 1);

    private ArcanumInAJarGameTests() {
    }

    @GameTest(template = TEMPLATE, batch = CONFIG_BATCH)
    public static void configuredItemsControlInsertionAndRemovalDrops(GameTestHelper helper) {
        try (var ignored = ApprenticeCodexServerConfig.useArcanumInAJarItemSettingsOverrideForGameTest(
                "minecraft:iron_ingot",
                "minecraft:diamond"
        )) {
            var blockEntity = placeJar(helper);
            var player = createPlayer(helper, "arcanum_jar_configured_items");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT, 2));

            var result = player.gameMode.useItemOn(
                    player,
                    helper.getLevel(),
                    player.getMainHandItem(),
                    InteractionHand.MAIN_HAND,
                    hitResult(helper)
            );

            helper.assertTrue(result.consumesAction(), "Configured material should be accepted");
            helper.assertTrue(blockEntity.getRemainingMaterialCount() == 1,
                    "Configured material insertion should add one pending operation");
            helper.assertTrue(player.getMainHandItem().getCount() == 1,
                    "Configured material insertion should consume one held item");

            loadCounts(helper, blockEntity, 2, 1, -1L, false);
            var drops = new ArrayList<ItemStack>();
            blockEntity.appendRemovalDrops(drops);
            helper.assertTrue(countItems(drops, Items.DIAMOND) == 2,
                    "Removal drops should use the configured product item");
            helper.assertTrue(countItems(drops, Items.IRON_INGOT) == 1,
                    "Removal drops should use the configured material item");
            helper.assertTrue(drops.stream().allMatch(stack -> stack.getCount() == 1),
                    "Internal contents should be emitted as single-item stacks");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = CONFIG_BATCH)
    public static void invalidItemSettingsBlockInteractionDropsButKeepProduction(GameTestHelper helper) {
        try (var ignoredItems = ApprenticeCodexServerConfig.useArcanumInAJarItemSettingsOverrideForGameTest(
                "apprenticecodex:missing_material",
                "minecraft:diamond"
        ); var ignoredTicks = ApprenticeCodexServerConfig
                .useArcanumInAJarTicksPerStoredParameterOverrideForGameTest(1)) {
            var blockEntity = placeJar(helper);
            var level = helper.getLevel();
            var progressStart = Math.max(0L, level.getGameTime() - 1L);
            loadCounts(helper, blockEntity, 1, 2, progressStart, false);

            var player = createPlayer(helper, "arcanum_jar_invalid_settings");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.REDSTONE, 2));
            var itemResult = player.gameMode.useItemOn(
                    player,
                    level,
                    player.getMainHandItem(),
                    InteractionHand.MAIN_HAND,
                    hitResult(helper)
            );
            helper.assertTrue(itemResult.consumesAction(), "Invalid settings should consume and reject item interaction");
            helper.assertTrue(player.getMainHandItem().getCount() == 2,
                    "Invalid settings should not consume the held item");

            var emptyHandResult = level.getBlockState(helper.absolutePos(JAR_POS))
                    .useWithoutItem(level, player, hitResult(helper));
            helper.assertTrue(emptyHandResult.consumesAction(),
                    "Invalid settings should consume and reject empty-hand interaction");
            helper.assertFalse(blockEntity.isDispensing(),
                    "Invalid settings should not start product dispensing");

            var drops = new ArrayList<ItemStack>();
            blockEntity.appendRemovalDrops(drops);
            helper.assertTrue(drops.isEmpty(),
                    "Invalid settings should discard internal contents when the jar is removed");

            ArcanumInAJarBlockEntity.serverTick(
                    level,
                    helper.absolutePos(JAR_POS),
                    level.getBlockState(helper.absolutePos(JAR_POS)),
                    blockEntity
            );
            helper.assertTrue(blockEntity.getStoredProductCount() == 2,
                    "Production should continue while item settings are invalid");
            helper.assertTrue(blockEntity.getRemainingMaterialCount() == 1,
                    "Production should consume pending material while item settings are invalid");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = CONFIG_BATCH)
    public static void dispensingReleasesEveryStoredProductAfterTenTicks(GameTestHelper helper) {
        var blockEntity = placeJar(helper);
        loadCounts(helper, blockEntity, 3, 0, -1L, false);
        blockEntity.startDispenseSequence();
        blockEntity.startDispenseSequence();

        helper.assertTrue(blockEntity.isDispensing(), "Dispensing should start once");
        helper.assertTrue(helper.getBlockState(JAR_POS).getValue(ArcanumInAJar.OPEN),
                "The jar should open while dispensing");

        helper.runAfterDelay(9, () -> helper.assertTrue(
                countNearbyItems(helper, ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get()) == 0,
                "Products should not be released before ten ticks"
        ));
        helper.runAfterDelay(11, () -> {
            helper.assertTrue(countNearbyItems(helper, ItemRegistry.CRYSTALLINE_ARCANE_SHARD.get()) == 3,
                    "All stored products should be released in the same dispensing cycle");
            helper.assertTrue(blockEntity.getStoredProductCount() == 0,
                    "Dispensing should consume every stored product");
            helper.assertFalse(blockEntity.isDispensing(),
                    "Dispensing should finish after the batch release");
            helper.assertFalse(helper.getBlockState(JAR_POS).getValue(ArcanumInAJar.OPEN),
                    "The jar should close after the batch release");
            helper.succeed();
        });
    }

    private static ArcanumInAJarBlockEntity placeJar(GameTestHelper helper) {
        helper.setBlock(JAR_POS, BlockRegistry.ARCANUM_IN_A_JAR.get());
        var blockEntity = helper.getBlockEntity(JAR_POS);
        helper.assertTrue(blockEntity instanceof ArcanumInAJarBlockEntity,
                "Arcanum in a Jar block entity was not created");
        return (ArcanumInAJarBlockEntity) blockEntity;
    }

    private static FakePlayer createPlayer(GameTestHelper helper, String name) {
        return new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
    }

    private static BlockHitResult hitResult(GameTestHelper helper) {
        var absolutePos = helper.absolutePos(JAR_POS);
        return new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false);
    }

    private static void loadCounts(
            GameTestHelper helper,
            ArcanumInAJarBlockEntity blockEntity,
            int storedProductCount,
            int remainingMaterialCount,
            long progressStartGameTime,
            boolean dispensing
    ) {
        var tag = new CompoundTag();
        tag.putInt("StoredParameterCount", storedProductCount);
        tag.putInt("RemainingOperationCount", remainingMaterialCount);
        if (progressStartGameTime >= 0L) {
            tag.putLong("ProgressStartGameTime", progressStartGameTime);
        }
        tag.putBoolean("Dispensing", dispensing);
        blockEntity.loadWithComponents(tag, helper.getLevel().registryAccess());
    }

    private static int countItems(Iterable<ItemStack> stacks, Item item) {
        var count = 0;
        for (var stack : stacks) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countNearbyItems(GameTestHelper helper, Item item) {
        var absolutePos = helper.absolutePos(JAR_POS);
        return helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class,
                        new AABB(absolutePos.above()).inflate(1.5D),
                        entity -> !entity.isRemoved() && entity.getItem().is(item)
                ).stream()
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }
}
