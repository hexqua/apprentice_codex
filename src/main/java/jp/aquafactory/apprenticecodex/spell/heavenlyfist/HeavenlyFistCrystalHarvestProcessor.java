package jp.aquafactory.apprenticecodex.spell.heavenlyfist;

import jp.aquafactory.apprenticecodex.item.curios.craftsmansdelight.CraftsmansDelight;
import jp.aquafactory.apprenticecodex.registry.TagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

final class HeavenlyFistCrystalHarvestProcessor {
    private HeavenlyFistCrystalHarvestProcessor() {
    }

    static void harvest(ServerLevel level, LivingEntity owner, Vec3 center, double radius) {
        if (!CraftsmansDelight.isEquippedBy(owner)) {
            return;
        }

        var searchRadius = Math.max(0, Mth.ceil(radius));
        var centerPos = BlockPos.containing(center);
        var minX = centerPos.getX() - searchRadius;
        var maxX = centerPos.getX() + searchRadius;
        var minY = Math.max(level.getMinBuildHeight(), centerPos.getY() - searchRadius);
        var maxY = Math.min(level.getMaxBuildHeight() - 1, centerPos.getY() + searchRadius);
        var minZ = centerPos.getZ() - searchRadius;
        var maxZ = centerPos.getZ() + searchRadius;
        var minChunkX = SectionPos.blockToSectionCoord(minX);
        var maxChunkX = SectionPos.blockToSectionCoord(maxX);
        var minChunkZ = SectionPos.blockToSectionCoord(minZ);
        var maxChunkZ = SectionPos.blockToSectionCoord(maxZ);
        var tool = CraftsmansDelight.createHeavenlyFistCrystalHarvestTool(owner);
        var mutablePos = new BlockPos.MutableBlockPos();

        // 外部 MOD の結晶は block class が揃わないため、タグと FACING だけで軽く判定する。
        for (var chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (var chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                var chunkMinX = Math.max(minX, chunk.getPos().getMinBlockX());
                var chunkMaxX = Math.min(maxX, chunk.getPos().getMaxBlockX());
                var chunkMinZ = Math.max(minZ, chunk.getPos().getMinBlockZ());
                var chunkMaxZ = Math.min(maxZ, chunk.getPos().getMaxBlockZ());
                var sections = chunk.getSections();
                for (var sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                    LevelChunkSection section = sections[sectionIndex];
                    if (section == null || section.hasOnlyAir()) {
                        continue;
                    }

                    var sectionY = level.getMinSection() + sectionIndex;
                    var sectionMinY = Math.max(minY, SectionPos.sectionToBlockCoord(sectionY));
                    var sectionMaxY = Math.min(maxY, SectionPos.sectionToBlockCoord(sectionY) + 15);
                    if (sectionMinY > sectionMaxY) {
                        continue;
                    }

                    for (var y = sectionMinY; y <= sectionMaxY; y++) {
                        for (var x = chunkMinX; x <= chunkMaxX; x++) {
                            for (var z = chunkMinZ; z <= chunkMaxZ; z++) {
                                mutablePos.set(x, y, z);
                                var state = chunk.getBlockState(mutablePos);
                                if (shouldHarvest(level, mutablePos, state)) {
                                    harvestBlock(level, owner, mutablePos.immutable(), state, tool);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean shouldHarvest(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.is(TagRegistry.Blocks.HEAVENLY_FIST_CRYSTAL_HARVEST_TARGETS)) {
            return false;
        }

        if (state.hasProperty(BlockStateProperties.FACING)) {
            return isHarvestSource(level, pos.relative(state.getValue(BlockStateProperties.FACING).getOpposite()));
        }

        for (var direction : Direction.values()) {
            if (isHarvestSource(level, pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHarvestSource(ServerLevel level, BlockPos pos) {
        return level.isInWorldBounds(pos)
                && level.getBlockState(pos).is(TagRegistry.Blocks.HEAVENLY_FIST_CRYSTAL_HARVEST_SOURCES);
    }

    private static void harvestBlock(ServerLevel level, LivingEntity owner, BlockPos pos, BlockState state,
                                     net.minecraft.world.item.ItemStack tool) {
        var blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        level.levelEvent(2001, pos, Block.getId(state));
        Block.dropResources(state, level, pos, blockEntity, owner, tool);
        state.spawnAfterBreak(level, pos, tool, true);
        if (level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL)) {
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(owner, state));
        }
    }
}
