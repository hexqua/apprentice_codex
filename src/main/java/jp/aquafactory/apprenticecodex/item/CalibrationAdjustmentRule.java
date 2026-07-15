package jp.aquafactory.apprenticecodex.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/** 候補の受理条件、重複制約、ヘルプを一か所で宣言するルール。 */
public final class CalibrationAdjustmentRule {
    private final Predicate<ItemStack> matcher;
    private final BiPredicate<ItemStack, ItemStack> conflict;
    private final CalibrationAdjustmentHint hint;

    private CalibrationAdjustmentRule(
            Predicate<ItemStack> matcher,
            BiPredicate<ItemStack, ItemStack> conflict,
            CalibrationAdjustmentHint hint
    ) {
        this.matcher = Objects.requireNonNull(matcher);
        this.conflict = Objects.requireNonNull(conflict);
        this.hint = Objects.requireNonNull(hint);
    }

    public static CalibrationAdjustmentRule repeatable(
            Predicate<ItemStack> matcher,
            CalibrationAdjustmentHint hint
    ) {
        return new CalibrationAdjustmentRule(matcher, (candidate, existing) -> false, hint);
    }

    public static CalibrationAdjustmentRule unique(
            Predicate<ItemStack> matcher,
            CalibrationAdjustmentHint hint
    ) {
        return new CalibrationAdjustmentRule(matcher, (candidate, existing) -> matcher.test(existing), hint);
    }

    public static <T> CalibrationAdjustmentRule uniqueBy(
            Predicate<ItemStack> matcher,
            Function<ItemStack, T> keyResolver,
            CalibrationAdjustmentHint hint
    ) {
        Objects.requireNonNull(keyResolver);
        return new CalibrationAdjustmentRule(
                matcher,
                (candidate, existing) -> matcher.test(existing)
                        && Objects.equals(keyResolver.apply(candidate), keyResolver.apply(existing)),
                hint
        );
    }

    public boolean accepts(@NotNull ItemStack stack) {
        return matcher.test(stack);
    }

    public boolean conflicts(@NotNull ItemStack candidate, @NotNull ItemStack existing) {
        return !existing.isEmpty() && conflict.test(candidate, existing);
    }

    public CalibrationAdjustmentHint hint() {
        return hint;
    }
}
