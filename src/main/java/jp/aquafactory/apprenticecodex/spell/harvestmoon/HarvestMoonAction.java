package jp.aquafactory.apprenticecodex.spell.harvestmoon;

import jp.aquafactory.apprenticecodex.block.comfortberrybush.ComfortBerryBushBlock;
import jp.aquafactory.apprenticecodex.utility.BlockTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface HarvestMoonAction {
    BlockPos anchorPos();

    int estimatedBlockCost();

    int execute(ServerLevel level, ServerPlayer player, ItemStack toolTemplate, Vec3 attractPos);

    final class RightClickCropAction implements HarvestMoonAction {
        private final BlockPos pos;
        private final ManualHarvestKind manualHarvestKind;

        RightClickCropAction(BlockPos pos, ManualHarvestKind manualHarvestKind) {
            this.pos = pos.immutable();
            this.manualHarvestKind = manualHarvestKind;
        }

        @Override
        public BlockPos anchorPos() {
            return pos;
        }

        @Override
        public int estimatedBlockCost() {
            return 1;
        }

        @Override
        public int execute(ServerLevel level, ServerPlayer player, ItemStack toolTemplate, Vec3 attractPos) {
            var state = level.getBlockState(pos);
            if (!HarvestMoonActionUtil.isStillEligibleForRightClickHarvest(level, pos, state, manualHarvestKind)) {
                return 0;
            }

            // mod 追加作物には CropBlock 継承でも独自の use 収穫を持つものがある。
            // 先に age リセット手動収穫へ落とすと、Farmer's Delight のトマトのような
            // rope 付き構造を壊し得るため、まずは通常の右クリック収穫を試す。
            var beforeIds = HarvestMoonActionUtil.captureNearbyItemIds(level, HarvestMoonActionUtil.createDropBox(pos));
            var result = HarvestMoonActionUtil.isFarmersDelightTomato(state)
                    ? BlockTools.useBlockByPlayerMainHand(level, player, pos, toolTemplate.copy(), Direction.UP)
                    : BlockTools.useItemOnBlockByPlayerMainHand(level, player, pos, toolTemplate.copy(), Direction.UP);
            var afterState = level.getBlockState(pos);
            var changedState = !afterState.equals(state);
            var movedDrops = HarvestMoonActionUtil.moveNewDropsTo(level, HarvestMoonActionUtil.createDropBox(pos), beforeIds, attractPos);
            if (result.consumesAction() || changedState || movedDrops > 0) {
                return 1;
            }

            if (manualHarvestKind == ManualHarvestKind.NONE) {
                return 0;
            }
            if (HarvestMoonActionUtil.shouldAvoidManualFallback(state)) {
                return 0;
            }

            return HarvestMoonActionUtil.executeManualAgeHarvest(level, player, pos, toolTemplate, attractPos, manualHarvestKind) ? 1 : 0;
        }
    }

    final class StemFruitAction implements HarvestMoonAction {
        private final BlockPos fruitPos;

        StemFruitAction(BlockPos fruitPos) {
            this.fruitPos = fruitPos.immutable();
        }

        @Override
        public BlockPos anchorPos() {
            return fruitPos;
        }

        @Override
        public int estimatedBlockCost() {
            return 1;
        }

        @Override
        public int execute(ServerLevel level, ServerPlayer player, ItemStack toolTemplate, Vec3 attractPos) {
            var state = level.getBlockState(fruitPos);
            if (!(state.getBlock() == Blocks.MELON || state.getBlock() == Blocks.PUMPKIN)
                    || !HarvestMoonActionUtil.hasAttachedStem(level, fruitPos)) {
                return 0;
            }

            var dropBox = HarvestMoonActionUtil.createDropBox(fruitPos).inflate(0.5);
            var beforeIds = HarvestMoonActionUtil.captureNearbyItemIds(level, dropBox);
            if (!BlockTools.tryBreakBlockByPlayerHands(level, player, fruitPos, toolTemplate.copy())) {
                return 0;
            }
            HarvestMoonActionUtil.moveNewDropsTo(level, dropBox, beforeIds, attractPos);
            return 1;
        }
    }

    final class ColumnHarvestAction implements HarvestMoonAction {
        private final BlockPos anchorPos;
        private final List<BlockPos> harvestTargets;

        ColumnHarvestAction(BlockPos anchorPos, List<BlockPos> harvestTargets) {
            this.anchorPos = anchorPos.immutable();
            this.harvestTargets = List.copyOf(harvestTargets);
        }

        @Override
        public BlockPos anchorPos() {
            return anchorPos;
        }

        @Override
        public int estimatedBlockCost() {
            return harvestTargets.size();
        }

        @Override
        public int execute(ServerLevel level, ServerPlayer player, ItemStack toolTemplate, Vec3 attractPos) {
            if (harvestTargets.isEmpty()) {
                return 0;
            }

            var box = new AABB(anchorPos).inflate(2.5);
            var beforeIds = HarvestMoonActionUtil.captureNearbyItemIds(level, box);
            var processed = 0;
            for (var target : harvestTargets) {
                var state = level.getBlockState(target);
                if (!HarvestMoonActionUtil.isColumnHarvestTarget(state)) {
                    continue;
                }
                if (BlockTools.tryBreakBlockByPlayerHands(level, player, target, toolTemplate.copy())) {
                    ++processed;
                }
            }

            if (processed > 0) {
                HarvestMoonActionUtil.moveNewDropsTo(level, box, beforeIds, attractPos);
            }
            return processed;
        }
    }

    final class ChorusHarvestAction implements HarvestMoonAction {
        private final BlockPos anchorPos;
        private final List<BlockPos> flowerTargets;
        private final List<BlockPos> plantTargets;
        private final AABB clusterBox;

        ChorusHarvestAction(BlockPos anchorPos, List<BlockPos> flowerTargets, List<BlockPos> plantTargets, AABB clusterBox) {
            this.anchorPos = anchorPos.immutable();
            this.flowerTargets = List.copyOf(flowerTargets);
            this.plantTargets = List.copyOf(plantTargets);
            this.clusterBox = clusterBox;
        }

        @Override
        public BlockPos anchorPos() {
            return anchorPos;
        }

        @Override
        public int estimatedBlockCost() {
            return flowerTargets.size() + plantTargets.size();
        }

        @Override
        public int execute(ServerLevel level, ServerPlayer player, ItemStack toolTemplate, Vec3 attractPos) {
            var beforeIds = HarvestMoonActionUtil.captureNearbyItemIds(level, clusterBox.inflate(1.0));
            var processed = 0;
            for (var pos : flowerTargets) {
                var state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof ChorusFlowerBlock)) {
                    continue;
                }
                if (BlockTools.tryBreakBlockByPlayerHands(level, player, pos, toolTemplate.copy())) {
                    ++processed;
                }
            }
            for (var pos : plantTargets) {
                var state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof ChorusPlantBlock)) {
                    continue;
                }
                if (BlockTools.tryBreakBlockByPlayerHands(level, player, pos, toolTemplate.copy())) {
                    ++processed;
                }
            }

            if (processed > 0) {
                HarvestMoonActionUtil.moveNewDropsTo(level, clusterBox.inflate(1.0), beforeIds, attractPos);
            }
            return processed;
        }
    }

    enum ManualHarvestKind {
        NONE,
        CROP,
        NETHER_WART,
        GENERIC_AGE
    }

    enum RootPreservingColumnKind {
        SUGAR_CANE,
        CACTUS,
        BAMBOO,
        KELP;

        static RootPreservingColumnKind of(BlockState state) {
            if (state.getBlock() instanceof SugarCaneBlock) {
                return SUGAR_CANE;
            }
            if (state.getBlock() instanceof CactusBlock) {
                return CACTUS;
            }
            if (state.getBlock() instanceof BambooStalkBlock || state.getBlock() instanceof BambooSaplingBlock) {
                return BAMBOO;
            }
            if (state.getBlock() instanceof KelpBlock || state.getBlock() instanceof KelpPlantBlock) {
                return KELP;
            }
            return null;
        }

        boolean isColumnMember(BlockState state) {
            return switch (this) {
                case SUGAR_CANE -> state.getBlock() instanceof SugarCaneBlock;
                case CACTUS -> state.getBlock() instanceof CactusBlock;
                case BAMBOO -> state.getBlock() instanceof BambooStalkBlock || state.getBlock() instanceof BambooSaplingBlock;
                case KELP -> state.getBlock() instanceof KelpBlock || state.getBlock() instanceof KelpPlantBlock;
            };
        }

        boolean isHarvestTarget(BlockState state) {
            return switch (this) {
                case SUGAR_CANE -> state.getBlock() instanceof SugarCaneBlock;
                case CACTUS -> state.getBlock() instanceof CactusBlock;
                case BAMBOO -> state.getBlock() instanceof BambooStalkBlock;
                case KELP -> state.getBlock() instanceof KelpBlock || state.getBlock() instanceof KelpPlantBlock;
            };
        }
    }

    final class HarvestMoonActionUtil {
        private HarvestMoonActionUtil() {
        }

        static boolean isStillEligibleForRightClickHarvest(LevelReader level, BlockPos pos, BlockState state, ManualHarvestKind manualHarvestKind) {
            if (state.getBlock() instanceof SweetBerryBushBlock) {
                return state.hasProperty(SweetBerryBushBlock.AGE) && state.getValue(SweetBerryBushBlock.AGE) >= 2;
            }
            if (state.getBlock() instanceof ComfortBerryBushBlock) {
                return state.hasProperty(ComfortBerryBushBlock.AGE)
                        && state.getValue(ComfortBerryBushBlock.AGE) >= ComfortBerryBushBlock.MAX_AGE;
            }
            if (manualHarvestKind == ManualHarvestKind.CROP && state.getBlock() instanceof CropBlock cropBlock) {
                return cropBlock.isMaxAge(state);
            }
            if (manualHarvestKind == ManualHarvestKind.NETHER_WART) {
                return state.getBlock() instanceof NetherWartBlock
                        && state.hasProperty(NetherWartBlock.AGE)
                        && state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
            }
            if (manualHarvestKind == ManualHarvestKind.GENERIC_AGE) {
                return isSupportedGenericAgeHarvest(level, pos, state);
            }
            return false;
        }

        static boolean executeManualAgeHarvest(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack toolTemplate,
                                               Vec3 attractPos, ManualHarvestKind manualHarvestKind) {
            var state = level.getBlockState(pos);
            var drops = switch (manualHarvestKind) {
                case CROP -> getCropDrops(level, player, pos, state, toolTemplate);
                case NETHER_WART -> getNetherWartDrops(level, player, pos, state, toolTemplate);
                case GENERIC_AGE -> getGenericAgeDrops(level, player, pos, state, toolTemplate);
                case NONE -> List.<ItemStack>of();
            };
            var resetState = switch (manualHarvestKind) {
                case CROP -> state.getBlock() instanceof CropBlock cropBlock ? cropBlock.getStateForAge(0) : null;
                case NETHER_WART -> state.hasProperty(NetherWartBlock.AGE) ? state.setValue(NetherWartBlock.AGE, 0) : null;
                case GENERIC_AGE -> resetGenericAgeState(state);
                case NONE -> null;
            };
            if (resetState == null) {
                return false;
            }

            var beforeIds = captureNearbyItemIds(level, createDropBox(pos));
            level.setBlock(pos, resetState, Block.UPDATE_ALL);
            level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE, pos);
            level.playSound(
                    null,
                    pos,
                    state.getSoundType(level, pos, player).getBreakSound(),
                    SoundSource.BLOCKS,
                    0.75f,
                    1.0f
            );
            for (var drop : drops) {
                Block.popResource(level, pos, drop);
            }
            moveNewDropsTo(level, createDropBox(pos), beforeIds, attractPos);
            return true;
        }

        static List<ItemStack> getCropDrops(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, ItemStack toolTemplate) {
            if (!(state.getBlock() instanceof CropBlock cropBlock) || !cropBlock.isMaxAge(state)) {
                return List.of();
            }

            var drops = new ArrayList<>(Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, toolTemplate.copy()));
            removeReplantSeed(drops, cropBlock.getCloneItemStack(level, pos, state));
            return drops;
        }

        static List<ItemStack> getNetherWartDrops(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, ItemStack toolTemplate) {
            if (!(state.getBlock() instanceof NetherWartBlock)
                    || !state.hasProperty(NetherWartBlock.AGE)
                    || state.getValue(NetherWartBlock.AGE) < NetherWartBlock.MAX_AGE) {
                return List.of();
            }

            var drops = new ArrayList<>(Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, toolTemplate.copy()));
            removeReplantSeed(drops, state.getBlock().getCloneItemStack(level, pos, state));
            return drops;
        }

        static List<ItemStack> getGenericAgeDrops(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, ItemStack toolTemplate) {
            if (!isSupportedGenericAgeHarvest(level, pos, state)) {
                return List.of();
            }

            var drops = new ArrayList<>(Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, toolTemplate.copy()));
            removeReplantSeed(drops, state.getBlock().getCloneItemStack(level, pos, state));
            return drops;
        }

        static void removeReplantSeed(List<ItemStack> drops, ItemStack seedStack) {
            if (seedStack.isEmpty()) {
                return;
            }

            for (var iterator = drops.iterator(); iterator.hasNext(); ) {
                var drop = iterator.next();
                if (!ItemStack.isSameItemSameTags(drop, seedStack)) {
                    continue;
                }
                drop.shrink(1);
                if (drop.isEmpty()) {
                    iterator.remove();
                }
                return;
            }
        }

        static BlockState resetGenericAgeState(BlockState state) {
            var property = findGenericAgeProperty(state);
            if (property == null) {
                return null;
            }
            return state.setValue(property, property.getPossibleValues().stream().min(Integer::compareTo).orElse(0));
        }

        static boolean isSupportedGenericAgeHarvest(LevelReader level, BlockPos pos, BlockState state) {
            if (!(state.getBlock() instanceof BushBlock)) {
                return false;
            }
            if (state.getBlock() instanceof CropBlock
                    || state.getBlock() instanceof StemBlock
                    || state.getBlock() instanceof SweetBerryBushBlock
                    || state.getBlock() instanceof NetherWartBlock
                    || state.getBlock() instanceof BambooStalkBlock
                    || state.getBlock() instanceof BambooSaplingBlock
                    || state.getBlock() instanceof SugarCaneBlock
                    || state.getBlock() instanceof CactusBlock
                    || state.getBlock() instanceof AttachedStemBlock) {
                return false;
            }

            var property = findGenericAgeProperty(state);
            if (property == null) {
                return false;
            }

            var max = property.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
            if (state.getValue(property) < max) {
                return false;
            }

            // 下段破壊が不利益になりやすい未知の縦長作物は、共通 fallback では扱わない。
            var above = level.getBlockState(pos.above());
            var below = level.getBlockState(pos.below());
            return above.getBlock() != state.getBlock() && below.getBlock() != state.getBlock();
        }

        static boolean shouldAvoidManualFallback(BlockState state) {
            return isFarmersDelightTomato(state);
        }

        static boolean isFarmersDelightTomato(BlockState state) {
            var blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            // Farmer's DelightのトマトだけはIssueでも報告されている不具合があるため特殊判定をする.
            return blockId != null
                    && "farmersdelight".equals(blockId.getNamespace())
                    && "tomatoes".equals(blockId.getPath());
        }

        static IntegerProperty findGenericAgeProperty(BlockState state) {
            IntegerProperty found = null;
            for (var property : state.getProperties()) {
                if (!(property instanceof IntegerProperty integerProperty)) {
                    continue;
                }
                if (!integerProperty.getName().contains("age")) {
                    continue;
                }
                var min = integerProperty.getPossibleValues().stream().min(Integer::compareTo).orElse(0);
                var max = integerProperty.getPossibleValues().stream().max(Integer::compareTo).orElse(0);
                if (min != 0 || max <= 0) {
                    continue;
                }
                if (found != null) {
                    return null;
                }
                found = integerProperty;
            }
            return found;
        }

        static boolean hasAttachedStem(LevelReader level, BlockPos fruitPos) {
            for (var direction : Direction.Plane.HORIZONTAL) {
                var neighbor = level.getBlockState(fruitPos.relative(direction));
                if (!(neighbor.getBlock() instanceof AttachedStemBlock)) {
                    continue;
                }
                if (neighbor.hasProperty(AttachedStemBlock.FACING)
                        // fruitPos.relative(direction) は「果実から見た茎の方向」なので、茎の FACING とは逆向きになる。
                        && direction.getOpposite() == neighbor.getValue(AttachedStemBlock.FACING)) {
                    return true;
                }
            }
            return false;
        }

        static boolean isColumnHarvestTarget(BlockState state) {
            var kind = RootPreservingColumnKind.of(state);
            return kind != null && kind.isHarvestTarget(state);
        }

        static AABB createDropBox(BlockPos pos) {
            return new AABB(pos).inflate(1.5);
        }

        static Set<Integer> captureNearbyItemIds(ServerLevel level, AABB box) {
            var ids = new HashSet<Integer>();
            for (var item : level.getEntitiesOfClass(ItemEntity.class, box, entity -> !entity.isRemoved())) {
                ids.add(item.getId());
            }
            return ids;
        }

        static int moveNewDropsTo(ServerLevel level, AABB box, Set<Integer> beforeIds, Vec3 attractPos) {
            var moved = 0;
            for (var item : level.getEntitiesOfClass(ItemEntity.class, box, entity -> !entity.isRemoved())) {
                if (beforeIds.contains(item.getId())) {
                    continue;
                }
                item.setPos(attractPos.x, attractPos.y, attractPos.z);
                item.setDeltaMovement(Vec3.ZERO);
                item.setNoPickUpDelay();
                item.hurtMarked = true;
                ++moved;
            }
            return moved;
        }

        static HarvestMoonAction createChorusAction(ServerLevel level, Set<BlockPos> cluster) {
            var flowers = new ArrayList<BlockPos>();
            var plants = new ArrayList<BlockPos>();
            var minX = Integer.MAX_VALUE;
            var minY = Integer.MAX_VALUE;
            var minZ = Integer.MAX_VALUE;
            var maxX = Integer.MIN_VALUE;
            var maxY = Integer.MIN_VALUE;
            var maxZ = Integer.MIN_VALUE;
            for (var pos : cluster) {
                var immutable = pos.immutable();
                var state = level.getBlockState(immutable);
                if (state.getBlock() instanceof ChorusFlowerBlock) {
                    flowers.add(immutable);
                } else if (state.getBlock() instanceof ChorusPlantBlock) {
                    plants.add(immutable);
                }
                minX = Math.min(minX, immutable.getX());
                minY = Math.min(minY, immutable.getY());
                minZ = Math.min(minZ, immutable.getZ());
                maxX = Math.max(maxX, immutable.getX());
                maxY = Math.max(maxY, immutable.getY());
                maxZ = Math.max(maxZ, immutable.getZ());
            }

            flowers.sort(Comparator.comparingInt((BlockPos blockPos) -> blockPos.getY()).reversed()
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getZ));
            plants.sort(Comparator.comparingInt((BlockPos blockPos) -> blockPos.getY()).reversed()
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getZ));

            var anchor = flowers.isEmpty() ? plants.get(0) : flowers.get(0);
            return new ChorusHarvestAction(
                    anchor,
                    flowers,
                    plants,
                    new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1)
            );
        }

        static int estimateDistanceSq(BlockPos origin, BlockPos pos) {
            var dx = pos.getX() - origin.getX();
            var dy = pos.getY() - origin.getY();
            var dz = pos.getZ() - origin.getZ();
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
