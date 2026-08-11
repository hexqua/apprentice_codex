package jp.aquafactory.apprenticecodex.block.alchemybrewer;

import io.redspace.ironsspellbooks.block.alchemist_cauldron.AlchemistCauldronTile;
import jp.aquafactory.apprenticecodex.config.block.AlchemyBrewerServerConfig;
import jp.aquafactory.apprenticecodex.utility.AlchemistCauldronFluidTools;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;

final class AlchemyBrewerWaterSupply {
    private static final int SEARCH_RANGE = 2;
    private static final int ALCHEMIST_CAULDRON_CAPACITY_MB = 1000;

    private AlchemyBrewerWaterSupply() {
    }

    static @Nullable BlockPos trySupply(ServerLevel level, BlockPos brewerPos,
                                        AlchemyBrewerServerConfig.Values config) {
        var candidates = new ArrayList<BlockPos>();
        for (var candidate : BlockPos.betweenClosed(
                brewerPos.offset(-SEARCH_RANGE, -SEARCH_RANGE, -SEARCH_RANGE),
                brewerPos.offset(SEARCH_RANGE, SEARCH_RANGE, SEARCH_RANGE)
        )) {
            if (candidate.equals(brewerPos) || !level.hasChunkAt(candidate)
                    || !isEligible(level, candidate, config)) {
                continue;
            }
            candidates.add(candidate.immutable());
        }

        candidates.sort(Comparator
                .comparingDouble((BlockPos candidate) -> candidate.distSqr(brewerPos))
                .thenComparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getZ)
                .thenComparingInt(BlockPos::getX));

        if (candidates.isEmpty()) {
            return null;
        }
        var target = candidates.getFirst();
        return supply(level, target, config) ? target : null;
    }

    private static boolean isEligible(ServerLevel level, BlockPos pos,
                                      AlchemyBrewerServerConfig.Values config) {
        var state = level.getBlockState(pos);
        if (config.vanillaCauldronWaterLevelIncrease() > 0) {
            if (state.is(Blocks.CAULDRON)) {
                return true;
            }
            if (state.is(Blocks.WATER_CAULDRON)
                    && state.getValue(LayeredCauldronBlock.LEVEL) < LayeredCauldronBlock.MAX_FILL_LEVEL) {
                return true;
            }
        }

        if (config.alchemistCauldronWaterAmountMb() <= 0
                || !(level.getBlockEntity(pos) instanceof AlchemistCauldronTile cauldron)) {
            return false;
        }
        var amount = AlchemistCauldronFluidTools.getTotalFluidAmount(cauldron);
        return amount < ALCHEMIST_CAULDRON_CAPACITY_MB
                && AlchemistCauldronFluidTools.containsOnlyWater(cauldron);
    }

    private static boolean supply(ServerLevel level, BlockPos pos,
                                  AlchemyBrewerServerConfig.Values config) {
        var state = level.getBlockState(pos);
        if (config.vanillaCauldronWaterLevelIncrease() > 0 && state.is(Blocks.CAULDRON)) {
            var levelIncrease = Math.min(
                    config.vanillaCauldronWaterLevelIncrease(),
                    LayeredCauldronBlock.MAX_FILL_LEVEL
            );
            level.setBlockAndUpdate(pos, Blocks.WATER_CAULDRON.defaultBlockState()
                    .setValue(LayeredCauldronBlock.LEVEL, levelIncrease));
            return true;
        }
        if (config.vanillaCauldronWaterLevelIncrease() > 0 && state.is(Blocks.WATER_CAULDRON)) {
            int currentLevel = state.getValue(LayeredCauldronBlock.LEVEL);
            var updatedLevel = Math.min(
                    LayeredCauldronBlock.MAX_FILL_LEVEL,
                    currentLevel + config.vanillaCauldronWaterLevelIncrease()
            );
            if (updatedLevel <= currentLevel) {
                return false;
            }
            level.setBlockAndUpdate(pos, state.setValue(LayeredCauldronBlock.LEVEL, updatedLevel));
            return true;
        }

        if (config.alchemistCauldronWaterAmountMb() <= 0
                || !(level.getBlockEntity(pos) instanceof AlchemistCauldronTile cauldron)
                || !AlchemistCauldronFluidTools.containsOnlyWater(cauldron)) {
            return false;
        }
        var filled = AlchemistCauldronFluidTools.fillWater(
                cauldron,
                config.alchemistCauldronWaterAmountMb(),
                IFluidHandler.FluidAction.EXECUTE
        );
        if (filled <= 0) {
            return false;
        }

        // Iron's のfluidInventory変更だけでは描画同期が保証されないため、BlockEntity更新を明示する。
        cauldron.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        return true;
    }
}
