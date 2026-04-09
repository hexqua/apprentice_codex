package jp.aquafactory.apprenticecodex.utility;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ErrandMageTradeHelper {
    private ErrandMageTradeHelper() {
    }

    public static boolean shouldIgnorePaymentTags(ItemStack stack) {
        return shouldIgnorePaymentTags(stack.getItem());
    }

    public static boolean shouldIgnorePaymentTags(Item item) {
        return item == io.redspace.ironsspellbooks.registries.ItemRegistry.TARNISHED_CROWN.get()
                || item == io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get();
    }

    public static ItemStack createPaymentStack(Item item, int count) {
        if (shouldIgnorePaymentTags(item)) {
            return new ItemStack(item, count);
        }
        return new ItemStack(item, count);
    }

    public static ItemStack createPaymentStack(ItemStack stack) {
        if (!shouldIgnorePaymentTags(stack)) {
            return stack.copy();
        }

        return new ItemStack(stack.getItem(), stack.getCount());
    }

    public static boolean matchesPaymentItem(ItemStack offer, ItemStack cost) {
        if (cost.isEmpty() && offer.isEmpty()) {
            return true;
        }

        return !cost.isEmpty() && !offer.isEmpty() && ItemStack.isSameItem(offer, cost);
    }
}
