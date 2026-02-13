package jp.aquafactory.apprenticecodex.common.spells.tinylumberjack;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.PriorityQueue;

public class TinyLumberjackJob {
    private static final int[][] OFFSETS = buildOffsets();
    private static final int LEAF_LOG_RADIUS = 4;
    private static final int LEAF_LOG_RADIUS_SQR = LEAF_LOG_RADIUS * LEAF_LOG_RADIUS;

    private final int originY;
    private final int logsPerTick;
    private final ArrayDeque<LogNode> logQueue = new ArrayDeque<>();
    private final LongOpenHashSet visitedLogs = new LongOpenHashSet();
    private final LongOpenHashSet logInfluence = new LongOpenHashSet();
    private final PriorityQueue<LeafNode> leafQueue =
            new PriorityQueue<>(Comparator.comparingInt(LeafNode::distance));
    private final LongOpenHashSet visitedLeaves = new LongOpenHashSet();
    private int maxLogDistanceProcessed;
    private ServerPlayer starter;
    private boolean complete;

    public TinyLumberjackJob(BlockPos originPos, int logsPerTick, ServerPlayer starter) {
        var originPos1 = originPos.immutable();
        this.logsPerTick = Math.max(1, logsPerTick);
        originY = originPos.getY();
        logQueue.add(new LogNode(originPos1, 0));
        visitedLogs.add(originPos1.asLong());
        addLogInfluence(originPos1);
        this.starter = starter;
    }

    public boolean isComplete() {
        return complete;
    }

    public void tick(ServerLevel level) {
        if (complete) {
            return;
        }

        var logsBroken = 0;
        var lastDistance = -1;
        while (logsBroken < logsPerTick && !logQueue.isEmpty()) {
            var node = logQueue.poll();
            var pos = node.pos();
            if (pos.getY() < originY) {
                continue;
            }

            var state = level.getBlockState(pos);
            var isLog = state.is(BlockTags.LOGS);
            if (isLog) {
                breakBlock(level, pos);
                ++logsBroken;
            }

            lastDistance = node.distance();
            enqueueLogNeighbors(level, pos, node.distance());
            if (isLog) {
                enqueueLeavesFromLog(level, pos, node.distance());
            }
        }

        if (lastDistance >= 0) {
            maxLogDistanceProcessed = Math.max(maxLogDistanceProcessed, lastDistance);
        }

        var leafDistanceLimit = logQueue.isEmpty() ? Integer.MAX_VALUE : maxLogDistanceProcessed;
        processLeaves(level, leafDistanceLimit);

        if (logQueue.isEmpty() && leafQueue.isEmpty()) {
            complete = true;
        }
    }

    private void enqueueLogNeighbors(ServerLevel level, BlockPos pos, int distance) {
        var mutable = new BlockPos.MutableBlockPos();
        for (var offset : OFFSETS) {
            mutable.setWithOffset(pos, offset[0], offset[1], offset[2]);
            if (mutable.getY() < originY) {
                continue;
            }

            var key = mutable.asLong();
            if (visitedLogs.contains(key)) {
                continue;
            }

            var state = level.getBlockState(mutable);
            if (!state.is(BlockTags.LOGS)) {
                continue;
            }

            visitedLogs.add(key);
            addLogInfluence(mutable);
            logQueue.add(new LogNode(mutable.immutable(), distance + 1));
        }
    }

    private void enqueueLeavesFromLog(ServerLevel level, BlockPos pos, int distance) {
        var mutable = new BlockPos.MutableBlockPos();
        for (var offset : OFFSETS) {
            mutable.setWithOffset(pos, offset[0], offset[1], offset[2]);
            enqueueLeaf(level, mutable, distance + 1);
        }
    }

    private void processLeaves(ServerLevel level, int distanceLimit) {
        while (!leafQueue.isEmpty()) {
            var node = leafQueue.peek();
            if (node.distance() > distanceLimit) {
                break;
            }

            leafQueue.poll();
            var pos = node.pos();
            var state = level.getBlockState(pos);
            if (isBreakableLeaf(state)) {
                breakBlock(level, pos);
            }

            enqueueLeafNeighbors(level, pos, node.distance());
        }
    }

    private void enqueueLeafNeighbors(ServerLevel level, BlockPos pos, int distance) {
        var mutable = new BlockPos.MutableBlockPos();
        for (var offset : OFFSETS) {
            mutable.setWithOffset(pos, offset[0], offset[1], offset[2]);
            enqueueLeaf(level, mutable, distance + 1);
        }
    }

    private void enqueueLeaf(ServerLevel level, BlockPos.MutableBlockPos pos, int distance) {
        var key = pos.asLong();
        if (visitedLeaves.contains(key)) {
            return;
        }

        if (!logInfluence.contains(key)) {
            return;
        }

        var state = level.getBlockState(pos);
        if (!isBreakableLeaf(state)) {
            return;
        }

        visitedLeaves.add(key);
        leafQueue.add(new LeafNode(pos.immutable(), distance));
    }

    private void breakBlock(ServerLevel level, BlockPos pos) {
        if (starter != null && !starter.isRemoved()) {
            var originalItem = starter.getMainHandItem();
            var state = level.getBlockState(pos);
            try{
                // 鉄の斧として伐採する.
                starter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
                starter.gameMode.destroyBlock(pos);

                // イベントを呼ばないと破壊演出が出ない.
                level.levelEvent(2001, pos, Block.getId(state));
            } finally {
                // すぐにアイテムをもとに戻す.
                starter.setItemInHand(InteractionHand.MAIN_HAND, originalItem);
            }
        } else {
            starter = null;
            level.destroyBlock(pos, true);
        }
    }

    private static boolean isBreakableLeaf(BlockState state) {
        if (!state.is(BlockTags.LEAVES)) {
            return false;
        }

        return !state.hasProperty(LeavesBlock.PERSISTENT) || !state.getValue(LeavesBlock.PERSISTENT);
    }

    private void addLogInfluence(BlockPos logPos) {
        var mutable = new BlockPos.MutableBlockPos();
        for (var dx = -LEAF_LOG_RADIUS; dx <= LEAF_LOG_RADIUS; ++dx) {
            for (var dy = -LEAF_LOG_RADIUS; dy <= LEAF_LOG_RADIUS; ++dy) {
                for (var dz = -LEAF_LOG_RADIUS; dz <= LEAF_LOG_RADIUS; ++dz) {
                    var distSqr = dx * dx + dy * dy + dz * dz;
                    if (distSqr > LEAF_LOG_RADIUS_SQR) {
                        continue;
                    }
                    mutable.set(logPos.getX() + dx, logPos.getY() + dy, logPos.getZ() + dz);
                    logInfluence.add(mutable.asLong());
                }
            }
        }
    }

    private static int[][] buildOffsets() {
        var offsets = new int[26][3];
        var index = 0;
        for (var dx = -1; dx <= 1; ++dx) {
            for (var dy = -1; dy <= 1; ++dy) {
                for (var dz = -1; dz <= 1; ++dz) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    offsets[index] = new int[]{dx, dy, dz};
                    ++index;
                }
            }
        }
        return offsets;
    }

    private record LogNode(BlockPos pos, int distance) {
    }

    private record LeafNode(BlockPos pos, int distance) {
    }
}
