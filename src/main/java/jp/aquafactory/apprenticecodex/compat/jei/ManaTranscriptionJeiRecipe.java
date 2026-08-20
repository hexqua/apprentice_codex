package jp.aquafactory.apprenticecodex.compat.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

record ManaTranscriptionJeiRecipe(
        ResourceLocation id,
        Mode mode,
        ItemStack target,
        List<ItemStack> operationItems,
        ItemStack targetResult,
        ItemStack secondaryResult
) {
    ManaTranscriptionJeiRecipe {
        target = target.copyWithCount(1);
        operationItems = operationItems.stream().map(stack -> stack.copyWithCount(1)).toList();
        if (operationItems.isEmpty()) {
            throw new IllegalArgumentException("Mana Transcription operation items must not be empty.");
        }
        targetResult = targetResult.copyWithCount(1);
        secondaryResult = secondaryResult.copy();
    }

    enum Mode {
        EXTRACTION,
        REPAIR_COST_RESET
    }
}
