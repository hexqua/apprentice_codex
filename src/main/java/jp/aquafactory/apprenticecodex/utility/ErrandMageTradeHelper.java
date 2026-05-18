package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.event.errandmage.ErrandMageTradeManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;

public final class ErrandMageTradeHelper {
    private ErrandMageTradeHelper() {
    }

    public static boolean shouldIgnorePaymentTags(ItemStack stack) {
        return shouldIgnorePaymentTags(stack.getItem());
    }

    public static boolean shouldIgnorePaymentTags(Item item) {
        return ErrandMageTradeManager.shouldIgnorePaymentTags(item);
    }

    public static ItemCost createPaymentStack(Item item, int count) {
        if (shouldIgnorePaymentTags(item)) {
            return new ItemCost(item, count);
        }
        return new ItemCost(item, count);
    }

    public static ItemCost createPaymentStack(ItemStack stack) {
        if (!shouldIgnorePaymentTags(stack)) {
            return new ItemCost(stack.getItem(), stack.getCount());
        }

        return new ItemCost(stack.getItem(), stack.getCount());
    }

    public static boolean matchesPaymentItem(ItemStack offer, ItemCost cost) {
        return !offer.isEmpty() && offer.is(cost.item());
    }
}
