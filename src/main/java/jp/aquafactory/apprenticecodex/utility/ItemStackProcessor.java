package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.world.item.ItemStack;

import java.util.Optional;

@FunctionalInterface
public interface ItemStackProcessor {
    Optional<ItemStackProcessingResult> process(ItemStack inputStack, int maxProcessCount);
}
