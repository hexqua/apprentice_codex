package jp.aquafactory.apprenticecodex.spell.harvestmoon;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class HarvestMoonTargetCollector {
    private HarvestMoonTargetCollector() {
    }

    static List<HarvestMoonAction> collect(ServerLevel level, ServerPlayer player, int range) {
        var origin = player.blockPosition();
        var visited = new LongOpenHashSet();
        var actions = new ArrayList<HarvestMoonAction>();

        for (var y = origin.getY() - 1; y <= origin.getY() + 3; ++y) {
            for (var x = origin.getX() - range; x <= origin.getX() + range; ++x) {
                for (var z = origin.getZ() - range; z <= origin.getZ() + range; ++z) {
                    if (x == origin.getX() && z == origin.getZ()) {
                        continue;
                    }

                    var pos = new BlockPos(x, y, z);
                    if (!level.isInWorldBounds(pos)) {
                        continue;
                    }
                    if (visited.contains(pos.asLong())) {
                        continue;
                    }

                    var state = level.getBlockState(pos);
                    var action = resolveAction(level, pos, state, visited);
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }
        }

        actions.sort(Comparator.comparingInt(action -> HarvestMoonAction.HarvestMoonActionUtil.estimateDistanceSq(origin, action.anchorPos())));
        return actions;
    }

    private static HarvestMoonAction resolveAction(ServerLevel level, BlockPos pos, BlockState state, LongOpenHashSet visited) {
        if (isDenylisted(state)) {
            visited.add(pos.asLong());
            return null;
        }

        var block = state.getBlock();

        if ((block == Blocks.MELON || block == Blocks.PUMPKIN)
                && HarvestMoonAction.HarvestMoonActionUtil.hasAttachedStem(level, pos)) {
            visited.add(pos.asLong());
            return new HarvestMoonAction.StemFruitAction(pos);
        }

        var columnKind = HarvestMoonAction.RootPreservingColumnKind.of(state);
        if (columnKind != null) {
            var action = createColumnAction(level, pos, columnKind, visited);
            if (action != null) {
                return action;
            }
        }

        if (block instanceof ChorusPlantBlock || block instanceof ChorusFlowerBlock) {
            var action = createChorusAction(level, pos, visited);
            if (action != null) {
                return action;
            }
        }

        if (block instanceof SweetBerryBushBlock) {
            if (state.hasProperty(SweetBerryBushBlock.AGE) && state.getValue(SweetBerryBushBlock.AGE) >= 2) {
                visited.add(pos.asLong());
                return new HarvestMoonAction.RightClickCropAction(pos, HarvestMoonAction.ManualHarvestKind.NONE);
            }
            return null;
        }

        if (block instanceof jp.aquafactory.apprenticecodex.block.comfortberrybush.ComfortBerryBushBlock) {
            if (state.hasProperty(jp.aquafactory.apprenticecodex.block.comfortberrybush.ComfortBerryBushBlock.AGE)
                    && state.getValue(jp.aquafactory.apprenticecodex.block.comfortberrybush.ComfortBerryBushBlock.AGE)
                    >= jp.aquafactory.apprenticecodex.block.comfortberrybush.ComfortBerryBushBlock.MAX_AGE) {
                visited.add(pos.asLong());
                return new HarvestMoonAction.RightClickCropAction(pos, HarvestMoonAction.ManualHarvestKind.NONE);
            }
            return null;
        }

        if (block instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
            visited.add(pos.asLong());
            return new HarvestMoonAction.RightClickCropAction(pos, HarvestMoonAction.ManualHarvestKind.CROP);
        }

        if (block instanceof NetherWartBlock
                && state.hasProperty(NetherWartBlock.AGE)
                && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE) {
            visited.add(pos.asLong());
            return new HarvestMoonAction.RightClickCropAction(pos, HarvestMoonAction.ManualHarvestKind.NETHER_WART);
        }

        if (HarvestMoonAction.HarvestMoonActionUtil.isSupportedGenericAgeHarvest(level, pos, state)) {
            visited.add(pos.asLong());
            return new HarvestMoonAction.RightClickCropAction(pos, HarvestMoonAction.ManualHarvestKind.GENERIC_AGE);
        }

        if (shouldAttemptTopOnlyRightClick(level, pos, state)) {
            visited.add(pos.asLong());
            return new HarvestMoonAction.RightClickCropAction(pos, HarvestMoonAction.ManualHarvestKind.NONE);
        }

        return null;
    }

    private static HarvestMoonAction createColumnAction(ServerLevel level, BlockPos pos,
                                                        HarvestMoonAction.RootPreservingColumnKind columnKind,
                                                        LongOpenHashSet visited) {
        var root = pos;
        while (root.getY() > level.getMinBuildHeight()) {
            var belowPos = root.below();
            var belowState = level.getBlockState(belowPos);
            if (columnKind.isColumnMember(belowState)) {
                root = belowPos;
                continue;
            }
            break;
        }

        var targets = new ArrayList<BlockPos>();
        var columnMembers = new ArrayList<BlockPos>();
        var current = root.above();
        while (level.isInWorldBounds(current)) {
            var currentState = level.getBlockState(current);
            if (!columnKind.isColumnMember(currentState)) {
                break;
            }
            if (isDenylisted(currentState)) {
                break;
            }
            var immutable = current.immutable();
            columnMembers.add(immutable);
            if (columnKind.isHarvestTarget(currentState)) {
                targets.add(immutable);
            }
            current = current.above();
        }

        if (targets.isEmpty()) {
            visited.add(root.asLong());
            return null;
        }

        targets.sort(Comparator.comparingInt((BlockPos blockPos) -> blockPos.getY()).reversed());
        visited.add(root.asLong());
        for (var member : columnMembers) {
            visited.add(member.asLong());
        }
        return new HarvestMoonAction.ColumnHarvestAction(targets.get(0), targets);
    }

    private static HarvestMoonAction createChorusAction(ServerLevel level, BlockPos startPos, LongOpenHashSet visited) {
        var cluster = new java.util.LinkedHashSet<BlockPos>();
        var queue = new ArrayDeque<BlockPos>();
        queue.add(startPos.immutable());
        visited.add(startPos.asLong());

        while (!queue.isEmpty()) {
            var current = queue.removeFirst();
            cluster.add(current);
            for (var direction : Direction.values()) {
                var neighbor = current.relative(direction);
                if (!level.isInWorldBounds(neighbor) || visited.contains(neighbor.asLong())) {
                    continue;
                }
                var neighborState = level.getBlockState(neighbor);
                if (!(neighborState.getBlock() instanceof ChorusPlantBlock) && !(neighborState.getBlock() instanceof ChorusFlowerBlock)) {
                    continue;
                }
                if (isDenylisted(neighborState)) {
                    visited.add(neighbor.asLong());
                    continue;
                }
                visited.add(neighbor.asLong());
                queue.addLast(neighbor.immutable());
            }
        }

        if (cluster.isEmpty()) {
            return null;
        }
        return HarvestMoonAction.HarvestMoonActionUtil.createChorusAction(level, cluster);
    }

    private static boolean shouldAttemptTopOnlyRightClick(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof NetherWartBlock
                || state.getBlock() instanceof SweetBerryBushBlock
                || state.getBlock() instanceof SugarCaneBlock
                || state.getBlock() instanceof CactusBlock
                || state.getBlock() instanceof BambooStalkBlock
                || state.getBlock() instanceof BambooSaplingBlock
                || state.getBlock() instanceof AttachedStemBlock) {
            return false;
        }

        var ageProperty = HarvestMoonAction.HarvestMoonActionUtil.findGenericAgeProperty(state);
        if (ageProperty == null) {
            return false;
        }

        var maxAge = ageProperty.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
        if (state.getValue(ageProperty) < maxAge) {
            return false;
        }

        var above = level.getBlockState(pos.above());
        var below = level.getBlockState(pos.below());
        return above.getBlock() != state.getBlock() && below.getBlock() == state.getBlock();
    }

    private static boolean isDenylisted(BlockState state) {
        return state.is(TagRegistry.Blocks.HARVEST_MOON_DENYLIST);
    }
}
