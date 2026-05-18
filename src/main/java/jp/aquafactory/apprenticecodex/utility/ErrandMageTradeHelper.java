package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.event.errandmage.ErrandMageTradeManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ErrandMageTradeHelper {
    private ErrandMageTradeHelper() {
    }

    public static boolean shouldIgnorePaymentTags(ItemStack stack) {
        return shouldIgnorePaymentTags(stack.getItem());
    }

    public static boolean shouldIgnorePaymentTags(Item item) {
        return ErrandMageTradeManager.shouldIgnorePaymentTags(item);
    }

    public static ItemStack createPaymentStack(Item item, int count) {
        var stack = new ItemStack(item, count);
        if (shouldIgnorePaymentTags(item)) {
            stack.setTag(null);
        }
        return stack;
    }

    public static ItemStack createPaymentStack(ItemStack stack) {
        if (!shouldIgnorePaymentTags(stack)) {
            return stack.copy();
        }

        return createPaymentStack(stack.getItem(), stack.getCount());
    }

    public static boolean matchesPaymentItem(ItemStack offer, ItemStack cost) {
        if (cost.isEmpty() && offer.isEmpty()) {
            return true;
        }

        return !cost.isEmpty() && !offer.isEmpty() && ItemStack.isSameItem(offer, cost);
    }
}
