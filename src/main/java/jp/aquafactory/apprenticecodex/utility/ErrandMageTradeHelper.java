package jp.aquafactory.apprenticecodex.utility;

import jp.aquafactory.apprenticecodex.event.errandmage.ErrandMageTradeManager;
import jp.aquafactory.apprenticecodex.event.errandmage.ErrandMageTradeStack;
import jp.aquafactory.apprenticecodex.registry.ItemRegistry;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.food.FoodProperties;

public final class ErrandMageTradeHelper {
    private static final int DEFAULT_COMFORT_BERRIES_PAYMENT_COUNT = 22;

    private ErrandMageTradeHelper() {
    }

    public static boolean shouldIgnorePaymentTags(ItemStack stack) {
        return shouldIgnorePaymentTags(stack.getItem());
    }

    public static boolean shouldIgnorePaymentTags(Item item) {
        return ErrandMageTradeManager.shouldIgnorePaymentTags(item);
    }

    public static ItemCost createPaymentStack(ErrandMageTradeStack definition, ItemStack stack) {
        if (definition.ignoreNbt() || definition.potion().isEmpty()) {
            return new ItemCost(stack.getItem(), stack.getCount());
        }

        var potionContents = stack.get(DataComponents.POTION_CONTENTS);
        if (potionContents == null) {
            throw new IllegalStateException("Errand Mage potion payment is missing potion contents: " + definition.item());
        }
        return new ItemCost(
                stack.getItemHolder(),
                stack.getCount(),
                DataComponentPredicate.builder()
                        .expect(DataComponents.POTION_CONTENTS, potionContents)
                        .build()
        );
    }

    public static boolean matchesPaymentItem(ItemStack offer, ItemCost cost) {
        return !offer.isEmpty() && offer.is(cost.item());
    }

    public static boolean matchesLegacyComfortBerriesPayment(ItemStack offer, ItemCost cost) {
        if (cost.count() != DEFAULT_COMFORT_BERRIES_PAYMENT_COUNT
                || !offer.is(ItemRegistry.COMFORT_BERRIES.get())
                || !offer.is(cost.item())
                || cost.components().test(offer)) {
            return false;
        }

        var expectedFood = cost.itemStack().get(DataComponents.FOOD);
        var actualFood = offer.get(DataComponents.FOOD);
        if (expectedFood == null || actualFood == null || !sameFoodProperties(expectedFood, actualFood)) {
            return false;
        }

        // 旧取引に保存された FOOD だけを復号後の値へ置き換え、その他の component 条件は維持する。
        var normalizedOffer = offer.copy();
        normalizedOffer.set(DataComponents.FOOD, expectedFood);
        return cost.components().test(normalizedOffer);
    }

    private static boolean sameFoodProperties(
            FoodProperties expected,
            FoodProperties actual
    ) {
        if (expected.nutrition() != actual.nutrition()
                || Float.compare(expected.saturation(), actual.saturation()) != 0
                || expected.canAlwaysEat() != actual.canAlwaysEat()
                || Float.compare(expected.eatSeconds(), actual.eatSeconds()) != 0
                || !sameStack(
                        expected.usingConvertsTo().orElse(ItemStack.EMPTY),
                        actual.usingConvertsTo().orElse(ItemStack.EMPTY)
                )
                || expected.effects().size() != actual.effects().size()) {
            return false;
        }

        for (var index = 0; index < expected.effects().size(); index++) {
            var expectedEffect = expected.effects().get(index);
            var actualEffect = actual.effects().get(index);
            if (Float.compare(expectedEffect.probability(), actualEffect.probability()) != 0
                    || !expectedEffect.effect().equals(actualEffect.effect())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameStack(ItemStack expected, ItemStack actual) {
        if (expected.isEmpty() || actual.isEmpty()) {
            return expected.isEmpty() && actual.isEmpty();
        }

        return expected.getCount() == actual.getCount()
                && ItemStack.isSameItemSameComponents(expected, actual);
    }
}
