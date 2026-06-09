package jp.aquafactory.apprenticecodex.compat.create;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import jp.aquafactory.apprenticecodex.config.ApprenticeCodexServerConfig;
import jp.aquafactory.apprenticecodex.utility.ItemStackProcessingResult;
import jp.aquafactory.apprenticecodex.utility.ItemStackProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

final class CreateExposedItemProcessingBridgeImpl {
    private CreateExposedItemProcessingBridgeImpl() {
    }

    static int processBlocks(
            ServerLevel level,
            Iterable<BlockPos> positions,
            int maxProcessCount,
            Set<Object> skipTransportedItems,
            ItemStackProcessor processor
    ) {
        if (maxProcessCount <= 0) {
            return 0;
        }

        var processedCount = 0;
        for (var pos : positions) {
            if (processedCount >= maxProcessCount) {
                break;
            }

            processedCount += processBlock(
                    level,
                    pos,
                    maxProcessCount - processedCount,
                    skipTransportedItems,
                    processor
            );
        }
        return processedCount;
    }

    static int processBasins(ServerLevel level, Iterable<BlockPos> positions, int maxProcessCount) {
        if (maxProcessCount <= 0) {
            return 0;
        }

        var processedCount = 0;
        for (var pos : positions) {
            if (processedCount >= maxProcessCount) {
                break;
            }

            processedCount += processBasin(level, pos, maxProcessCount - processedCount);
        }
        return processedCount;
    }

    private static int processBasin(ServerLevel level, BlockPos pos, int maxProcessCount) {
        if (maxProcessCount <= 0 || !(level.getBlockEntity(pos) instanceof BasinBlockEntity basin)) {
            return 0;
        }

        var processedCount = 0;
        while (processedCount < maxProcessCount) {
            var recipe = findCompactingRecipe(level, basin);
            if (recipe == null || !BasinRecipe.apply(basin, recipe)) {
                break;
            }

            notifyBasinContentsChanged(basin);
            processedCount++;
        }
        return processedCount;
    }

    private static void notifyBasinContentsChanged(BasinBlockEntity basin) {
        try {
            // Create の追加依存型を javac に露出しないため、更新通知だけ反射で呼ぶ。
            ((Object) basin).getClass().getMethod("notifyChangeOfContents").invoke(basin);
        } catch (ReflectiveOperationException ignored) {
            // リフレクション失敗しても握りつぶし.
        }
    }

    private static Recipe<?> findCompactingRecipe(ServerLevel level, BasinBlockEntity basin) {
        var compactingType = AllRecipeTypes.COMPACTING.getType();
        return level.getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.value().getType() == compactingType)
                .filter(recipe -> !ApprenticeCodexServerConfig.isHeavenlyFistCreateRecipeDenied(recipe.id()))
                .sorted(Comparator.comparingInt((RecipeHolder<?> recipe) -> recipe.value().getIngredients().size()).reversed())
                .filter(recipe -> BasinRecipe.match(basin, recipe.value()))
                .map(RecipeHolder::value)
                .findFirst()
                .orElse(null);
    }

    private static int processBlock(
            ServerLevel level,
            BlockPos pos,
            int maxProcessCount,
            Set<Object> skipTransportedItems,
            ItemStackProcessor processor
    ) {
        if (maxProcessCount <= 0 || !isSupportedExposedHolder(level, pos)) {
            return 0;
        }

        var handler = BlockEntityBehaviour.get(level, pos, TransportedItemStackHandlerBehaviour.TYPE);
        if (handler == null) {
            return 0;
        }

        var processedCount = new int[]{0};
        handler.handleProcessingOnAllItems(transportedItem -> {
            var remainingBudget = maxProcessCount - processedCount[0];
            if (remainingBudget <= 0) {
                return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
            }

            var result = processTransportedItem(transportedItem, remainingBudget, skipTransportedItems, processor);
            processedCount[0] += result.processedCount();
            return result.transportedResult();
        });
        return processedCount[0];
    }

    private static ProcessedTransportedResult processTransportedItem(
            TransportedItemStack transportedItem,
            int maxProcessCount,
            Set<Object> skipTransportedItems,
            ItemStackProcessor processor
    ) {
        if (skipTransportedItems.contains(transportedItem) || transportedItem.stack.isEmpty()) {
            return ProcessedTransportedResult.doNothing();
        }

        var processingResult = processor.process(transportedItem.stack, maxProcessCount);
        if (processingResult.isEmpty() || processingResult.get().processedCount() <= 0) {
            return ProcessedTransportedResult.doNothing();
        }

        var result = processingResult.get();
        var convertedItems = buildConvertedItems(transportedItem, result, skipTransportedItems);
        if (convertedItems.isEmpty()) {
            return new ProcessedTransportedResult(
                    result.processedCount(),
                    TransportedItemStackHandlerBehaviour.TransportedResult.removeItem()
            );
        }

        return new ProcessedTransportedResult(
                result.processedCount(),
                TransportedItemStackHandlerBehaviour.TransportedResult.convertTo(convertedItems)
        );
    }

    private static List<TransportedItemStack> buildConvertedItems(
            TransportedItemStack source,
            ItemStackProcessingResult result,
            Set<Object> skipTransportedItems
    ) {
        var convertedItems = new ArrayList<TransportedItemStack>();
        var remainingCount = source.stack.getCount() - Math.min(result.processedCount(), source.stack.getCount());
        if (remainingCount > 0) {
            var remainingStack = source.stack.copy();
            remainingStack.setCount(remainingCount);
            convertedItems.add(copyWithStack(source, remainingStack, skipTransportedItems, false));
        }

        for (var outputStack : result.outputStacks()) {
            if (outputStack.isEmpty() || outputStack.getCount() <= 0) {
                continue;
            }
            convertedItems.add(copyWithStack(source, outputStack.copy(), skipTransportedItems, true));
        }
        return convertedItems;
    }

    private static TransportedItemStack copyWithStack(
            TransportedItemStack source,
            ItemStack stack,
            Set<Object> skipTransportedItems,
            boolean skipFurtherProcessing
    ) {
        var copy = source.copy();
        copy.stack = stack;
        copy.clearFanProcessingData();
        if (skipFurtherProcessing) {
            skipTransportedItems.add(copy);
        }
        return copy;
    }

    private static boolean isSupportedExposedHolder(ServerLevel level, BlockPos pos) {
        var blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof BeltBlockEntity
                || blockEntity instanceof DepotBlockEntity
                || blockEntity instanceof EjectorBlockEntity;
    }

    private record ProcessedTransportedResult(
            int processedCount,
            TransportedItemStackHandlerBehaviour.TransportedResult transportedResult
    ) {
        private static ProcessedTransportedResult doNothing() {
            return new ProcessedTransportedResult(
                    0,
                    TransportedItemStackHandlerBehaviour.TransportedResult.doNothing()
            );
        }
    }
}
