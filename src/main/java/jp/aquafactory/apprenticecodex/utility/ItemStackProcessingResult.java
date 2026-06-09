package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ItemStackProcessingResult(int processedCount, List<ItemStack> outputStacks) {
    public ItemStackProcessingResult {
        processedCount = Math.max(0, processedCount);
        outputStacks = List.copyOf(outputStacks);
    }
}
