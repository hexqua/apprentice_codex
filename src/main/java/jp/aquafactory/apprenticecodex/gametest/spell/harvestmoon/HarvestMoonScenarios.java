package jp.aquafactory.apprenticecodex.gametest.spell.harvestmoon;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import jp.aquafactory.apprenticecodex.registry.SpellRegistry;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;

import static jp.aquafactory.apprenticecodex.gametest.support.BlockStateTestSupport.*;
import static jp.aquafactory.apprenticecodex.gametest.support.CurioTestSupport.equipRingCurio;
import static jp.aquafactory.apprenticecodex.gametest.support.PlayerTestSupport.createHarvestMoonPlayer;
import static jp.aquafactory.apprenticecodex.gametest.support.RegistryTestSupport.requireForgeBlock;
import static jp.aquafactory.apprenticecodex.gametest.support.RegistryTestSupport.requireForgeItem;

final class HarvestMoonScenarios {
    private HarvestMoonScenarios() {}

    static final String FARMERS_DELIGHT_MOD_ID = "farmersdelight";

    static final ResourceLocation FARMERS_DELIGHT_TOMATO_BLOCK =
            ResourceLocation.fromNamespaceAndPath(FARMERS_DELIGHT_MOD_ID, "tomatoes");

    static final ResourceLocation FARMERS_DELIGHT_TOMATO_ITEM =
            ResourceLocation.fromNamespaceAndPath(FARMERS_DELIGHT_MOD_ID, "tomato");

    static void harvestMoonResetsMatureNetherWartAndPullsDrops(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var matureCropPos = new BlockPos(3, 2, 0);
        var immatureCropPos = new BlockPos(4, 2, 0);
        // age リセット収穫の検証が目的。
        // 小麦系は GameTest/FakePlayer 環境だと明るさ・耕地維持・更新順のノイズを受けやすいため、
        // ここでは Soul Sand だけで生存条件を満たせるネザーウォートで収穫ロジック自体を検証する。
        helper.setBlock(matureCropPos.below(), Blocks.SOUL_SAND);
        helper.setBlock(immatureCropPos.below(), Blocks.SOUL_SAND);
        helper.setBlock(matureCropPos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE));
        helper.setBlock(immatureCropPos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 2));

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));
        helper.runAtTickTime(3, () -> {
            helper.assertBlockProperty(matureCropPos, NetherWartBlock.AGE, 0);
            helper.assertBlockProperty(immatureCropPos, NetherWartBlock.AGE, 2);
            helper.assertItemEntityPresent(Items.NETHER_WART, casterPos, 1.5);
            helper.assertItemEntityNotPresent(Items.NETHER_WART, matureCropPos, 1.5);
            helper.succeed();
        });
    }

    static void harvestMoonWithCraftsmansDelightShearsSheepWithoutShears(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var redSheepPos = new BlockPos(3, 2, 0);
        var blueSheepPos = new BlockPos(5, 2, 0);
        var redSheep = helper.spawn(EntityType.SHEEP, redSheepPos);
        var blueSheep = helper.spawn(EntityType.SHEEP, blueSheepPos);
        redSheep.setNoAi(true);
        blueSheep.setNoAi(true);
        redSheep.setColor(DyeColor.RED);
        blueSheep.setColor(DyeColor.BLUE);

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(redSheep.isSheared() && blueSheep.isSheared(),
                    "HarvestMoon should shear every captured sheep without real shears");
            helper.assertItemEntityPresent(Items.RED_WOOL, casterPos, 1.5);
            helper.assertItemEntityPresent(Items.BLUE_WOOL, casterPos, 1.5);
            helper.assertItemEntityNotPresent(Items.RED_WOOL, redSheepPos, 1.5);
            helper.assertItemEntityNotPresent(Items.BLUE_WOOL, blueSheepPos, 1.5);
            helper.assertTrue(player.getMainHandItem().is(Items.STICK),
                    "HarvestMoon should restore the caster's main-hand item after virtual shearing");
            helper.succeed();
        });
    }

    static void harvestMoonWithoutCraftsmansDelightDoesNotShearSheep(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var sheep = helper.spawn(EntityType.SHEEP, new BlockPos(3, 2, 0));
        sheep.setNoAi(true);
        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));

        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(!sheep.isSheared(), "HarvestMoon should not shear sheep without Craftsman's Delight");
            helper.assertItemEntityNotPresent(Items.WHITE_WOOL, casterPos, 1.5);
            helper.succeed();
        });
    }

    static void harvestMoonSheepTargetsAreFixedAtCastAndRecheckedWhenProcessed(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var adult = helper.spawn(EntityType.SHEEP, new BlockPos(3, 2, 0));
        var alreadySheared = helper.spawn(EntityType.SHEEP, new BlockPos(5, 2, 0));
        var baby = helper.spawn(EntityType.SHEEP, new BlockPos(7, 2, 0));
        adult.setNoAi(true);
        alreadySheared.setNoAi(true);
        baby.setNoAi(true);
        adult.setColor(DyeColor.RED);
        alreadySheared.setColor(DyeColor.BLUE);
        baby.setColor(DyeColor.YELLOW);
        baby.setAge(-24000);

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        equipRingCurio(player, new ItemStack(ItemRegistry.CRAFTSMANS_DELIGHT.get()));
        var newcomer = new Sheep[1];
        helper.runAtTickTime(1, () -> {
            castHarvestMoon(helper, player, 1);
            alreadySheared.setSheared(true);
            // 詠唱後に範囲へ入ったヒツジは、ジョブの対象リストに加わらない。
            newcomer[0] = helper.spawn(EntityType.SHEEP, new BlockPos(9, 2, 0));
            newcomer[0].setNoAi(true);
            newcomer[0].setColor(DyeColor.GREEN);
        });
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(adult.isSheared(), "HarvestMoon should shear the adult captured at cast time");
            helper.assertTrue(alreadySheared.isSheared(), "HarvestMoon should leave sheep sheared before job execution unchanged");
            helper.assertTrue(!baby.isSheared(), "HarvestMoon should not shear baby sheep");
            helper.assertTrue(!newcomer[0].isSheared(), "HarvestMoon should not shear sheep entering after the cast");
            helper.assertItemEntityPresent(Items.RED_WOOL, casterPos, 1.5);
            helper.assertItemEntityNotPresent(Items.BLUE_WOOL, casterPos, 1.5);
            helper.assertItemEntityNotPresent(Items.YELLOW_WOOL, casterPos, 1.5);
            helper.assertItemEntityNotPresent(Items.GREEN_WOOL, casterPos, 1.5);
            helper.succeed();
        });
    }

    static void harvestMoonHarvestsFarmersDelightTomatoViaRightClick(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var tomatoPos = new BlockPos(3, 2, 0);

        if (!ModList.get().isLoaded(FARMERS_DELIGHT_MOD_ID)) {
            helper.succeed();
            return;
        }

        var tomatoBlock = requireForgeBlock(helper, FARMERS_DELIGHT_TOMATO_BLOCK);
        var tomatoItem = requireForgeItem(helper, FARMERS_DELIGHT_TOMATO_ITEM);
        helper.setBlock(tomatoPos.below(), Blocks.FARMLAND);
        helper.setBlock(tomatoPos, withIntegerProperty(helper, tomatoBlock.defaultBlockState(), "age", 3));

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));

        helper.runAtTickTime(3, () -> {
            var harvestedState = helper.getBlockState(tomatoPos);
            helper.assertTrue(harvestedState.is(tomatoBlock), "Farmer's Delight tomato should remain planted after HarvestMoon");
            helper.assertTrue(getIntegerPropertyValue(helper, harvestedState, "age") == 0,
                    "Farmer's Delight tomato should reset to age 0 after HarvestMoon but got " + harvestedState);
            helper.assertItemEntityPresent(tomatoItem, casterPos, 1.5);
            helper.assertItemEntityNotPresent(tomatoItem, tomatoPos, 1.5);
            helper.succeed();
        });
    }

    static void harvestMoonKeepsFarmersDelightTomatoRopeState(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var baseTomatoPos = new BlockPos(3, 2, 0);
        var ropeTomatoPos = baseTomatoPos.above();

        if (!ModList.get().isLoaded(FARMERS_DELIGHT_MOD_ID)) {
            helper.succeed();
            return;
        }

        var tomatoBlock = requireForgeBlock(helper, FARMERS_DELIGHT_TOMATO_BLOCK);
        var tomatoItem = requireForgeItem(helper, FARMERS_DELIGHT_TOMATO_ITEM);
        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.setBlock(baseTomatoPos.below(), Blocks.FARMLAND);
        // rope 付きトマトは上段で天井扱いになりやすいため、GameTest でも補助光を置く。
        helper.setBlock(baseTomatoPos.east(), Blocks.GLOWSTONE);
        helper.setBlock(baseTomatoPos, withIntegerProperty(helper, tomatoBlock.defaultBlockState(), "age", 0));
        helper.setBlock(
                ropeTomatoPos,
                withBooleanProperty(
                        helper,
                        withIntegerProperty(helper, tomatoBlock.defaultBlockState(), "age", 3),
                        "ropelogged",
                        true
                )
        );

        // HarvestMoon が依存している右クリック経路そのものを直接通し、
        // 成熟した rope 付き上段トマトを収穫しても rope 状態が壊れないことを検証する。
        var result = BlockTools.useBlockByPlayerMainHand(helper.getLevel(), player, helper.absolutePos(ropeTomatoPos), new ItemStack(Items.STICK));
        helper.assertTrue(result.consumesAction(), "Farmer's Delight rope tomato block use should consume the action but got " + result);

        var baseState = helper.getBlockState(baseTomatoPos);
        var ropeState = helper.getBlockState(ropeTomatoPos);
        helper.assertTrue(baseState.is(tomatoBlock), "Lower tomato support should remain planted after right click harvest but got " + baseState);
        helper.assertTrue(getIntegerPropertyValue(helper, baseState, "age") == 0,
                "Lower tomato support should stay at age 0 after right click harvest but got " + baseState);
        helper.assertTrue(ropeState.is(tomatoBlock),
                "Harvested rope tomato should stay planted after right click harvest but got " + ropeState);
        helper.assertTrue(getBooleanPropertyValue(helper, ropeState, "ropelogged"),
                "Right click harvest should preserve Farmer's Delight tomato rope state but got " + ropeState);
        helper.assertTrue(getIntegerPropertyValue(helper, ropeState, "age") == 0,
                "Harvested rope tomato should reset to age 0 after right click harvest but got " + ropeState);
        helper.assertItemEntityPresent(tomatoItem, ropeTomatoPos, 1.5);
        helper.succeed();
    }

    static void harvestMoonHarvestsStemFruitWithoutBreakingStem(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 3, 0);
        var stemPos = new BlockPos(2, 2, 1);
        var fruitPos = new BlockPos(3, 2, 1);
        helper.setBlock(stemPos.below(), Blocks.FARMLAND);
        helper.setBlock(stemPos, Blocks.ATTACHED_MELON_STEM.defaultBlockState().setValue(AttachedStemBlock.FACING, Direction.EAST));
        helper.setBlock(fruitPos, Blocks.MELON);

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));

        helper.succeedWhen(() -> {
            helper.assertBlockNotPresent(Blocks.MELON, fruitPos);
            helper.assertBlockPresent(Blocks.MELON_STEM, stemPos);
            helper.assertItemEntityPresent(Items.MELON_SLICE, casterPos, 1.5);
        });
    }

    static void harvestMoonProcessesTargetsAcrossMultipleTicksAndKeepsBambooRoot(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 4, 0);
        var bambooBase = new BlockPos(4, 2, 1);
        helper.setBlock(bambooBase.below(), Blocks.DIRT);
        helper.setBlock(bambooBase, Blocks.BAMBOO);
        for (var offset = 1; offset <= 6; ++offset) {
            helper.setBlock(bambooBase.above(offset), Blocks.BAMBOO);
        }

        var cropPositions = new ArrayList<BlockPos>();
        for (var x = 2; x <= 9; ++x) {
            for (var z = -4; z <= 4; ++z) {
                var pos = new BlockPos(x, 3, z);
                if (pos.getX() == bambooBase.getX() && pos.getZ() == bambooBase.getZ()) {
                    continue;
                }
                cropPositions.add(pos);
                helper.setBlock(pos.below(), Blocks.SOUL_SAND);
                helper.setBlock(pos, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, NetherWartBlock.MAX_AGE));
            }
        }
        helper.assertTrue(cropPositions.size() > 64, "HarvestMoon tick budget test requires more than 64 crops");

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));
        helper.runAtTickTime(2, () -> helper.assertTrue(countMatureHarvestMoonPlants(helper, cropPositions) > 0,
                "HarvestMoon should leave some crops for later ticks"));

        helper.succeedWhen(() -> {
            helper.assertTrue(countMatureHarvestMoonPlants(helper, cropPositions) == 0,
                    "HarvestMoon should eventually harvest every queued mature crop");
            helper.assertBlockPresent(Blocks.BAMBOO, bambooBase);
            for (var offset = 1; offset <= 6; ++offset) {
                helper.assertBlockNotPresent(Blocks.BAMBOO, bambooBase.above(offset));
            }
            helper.assertItemEntityPresent(Items.BAMBOO, casterPos, 1.5);
        });
    }

    static void harvestMoonHarvestsKelpColumnBeyondInitialYSlice(GameTestHelper helper) {
        var casterPos = new BlockPos(0, 4, 0);
        var kelpBase = new BlockPos(3, 2, 0);
        helper.setBlock(kelpBase.below(), Blocks.DIRT);
        helper.setBlock(kelpBase, Blocks.KELP_PLANT);
        for (var offset = 1; offset <= 5; ++offset) {
            helper.setBlock(kelpBase.above(offset), Blocks.KELP_PLANT);
        }
        helper.setBlock(kelpBase.above(6), Blocks.KELP);

        var player = createHarvestMoonPlayer(helper, casterPos, new ItemStack(Items.STICK));
        helper.runAtTickTime(1, () -> castHarvestMoon(helper, player, 1));

        helper.succeedWhen(() -> {
            var baseState = helper.getBlockState(kelpBase);
            helper.assertTrue(baseState.is(Blocks.KELP) || baseState.is(Blocks.KELP_PLANT),
                    "HarvestMoon should keep the bottom kelp block");
            for (var offset = 1; offset <= 6; ++offset) {
                var state = helper.getBlockState(kelpBase.above(offset));
                helper.assertTrue(!state.is(Blocks.KELP) && !state.is(Blocks.KELP_PLANT),
                        "HarvestMoon should remove upper kelp blocks even outside the initial Y slice");
            }
            helper.assertItemEntityPresent(Items.KELP, casterPos, 1.5);
        });
    }

    static void castHarvestMoon(GameTestHelper helper, FakePlayer player, int spellLevel) {
        var spell = SpellRegistry.HARVEST_MOON.get();
        spell.onCast(helper.getLevel(), spellLevel, player, CastSource.SPELLBOOK, MagicData.getPlayerMagicData(player));
    }

    static int countMatureHarvestMoonPlants(GameTestHelper helper, List<BlockPos> cropPositions) {
        var count = 0;
        for (var pos : cropPositions) {
            var state = helper.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
                ++count;
                continue;
            }
            if (state.is(Blocks.NETHER_WART)
                    && state.hasProperty(NetherWartBlock.AGE)
                    && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE) {
                ++count;
            }
        }
        return count;
    }
}
