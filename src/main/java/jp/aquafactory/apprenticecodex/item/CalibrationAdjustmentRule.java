package jp.aquafactory.apprenticecodex.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/** 候補の受理条件、重複制約、ヘルプを一か所で宣言するルール。 */
public final class CalibrationAdjustmentRule {
    public enum DuplicatePolicy {
        REPEATABLE,
        UNIQUE_RULE,
        UNIQUE_KEY
    }

    private final String displayId;
    private final Predicate<ItemStack> matcher;
    private final BiPredicate<ItemStack, ItemStack> conflict;
    private final CalibrationAdjustmentHint hint;
    private final DuplicatePolicy duplicatePolicy;
    private final CalibrationConstraintDisplay constraintDisplay;

    private CalibrationAdjustmentRule(
            String displayId,
            Predicate<ItemStack> matcher,
            BiPredicate<ItemStack, ItemStack> conflict,
            CalibrationAdjustmentHint hint,
            DuplicatePolicy duplicatePolicy,
            CalibrationConstraintDisplay constraintDisplay
    ) {
        this.displayId = Objects.requireNonNull(displayId);
        if (displayId.isBlank()) {
            throw new IllegalArgumentException("Calibration adjustment display ID must not be blank.");
        }
        this.matcher = Objects.requireNonNull(matcher);
        this.conflict = Objects.requireNonNull(conflict);
        this.hint = Objects.requireNonNull(hint);
        this.duplicatePolicy = Objects.requireNonNull(duplicatePolicy);
        this.constraintDisplay = Objects.requireNonNull(constraintDisplay);
    }

    public static CalibrationAdjustmentRule repeatable(
            String displayId,
            Predicate<ItemStack> matcher,
            CalibrationAdjustmentHint hint
    ) {
        return new CalibrationAdjustmentRule(
                displayId,
                matcher,
                (candidate, existing) -> false,
                hint,
                DuplicatePolicy.REPEATABLE,
                CalibrationConstraintDisplay.none()
        );
    }

    public static CalibrationAdjustmentRule unique(
            String displayId,
            Predicate<ItemStack> matcher,
            CalibrationAdjustmentHint hint
    ) {
        return unique(displayId, matcher, hint, CalibrationConstraintDisplay.sameEffect());
    }

    public static CalibrationAdjustmentRule unique(
            String displayId,
            Predicate<ItemStack> matcher,
            CalibrationAdjustmentHint hint,
            CalibrationConstraintDisplay constraintDisplay
    ) {
        return new CalibrationAdjustmentRule(
                displayId,
                matcher,
                (candidate, existing) -> matcher.test(existing),
                hint,
                DuplicatePolicy.UNIQUE_RULE,
                constraintDisplay
        );
    }

    public static <T> CalibrationAdjustmentRule uniqueBy(
            String displayId,
            Predicate<ItemStack> matcher,
            Function<ItemStack, T> keyResolver,
            CalibrationAdjustmentHint hint
    ) {
        return uniqueBy(
                displayId,
                matcher,
                keyResolver,
                hint,
                CalibrationConstraintDisplay.sameEffect()
        );
    }

    public static <T> CalibrationAdjustmentRule uniqueBy(
            String displayId,
            Predicate<ItemStack> matcher,
            Function<ItemStack, T> keyResolver,
            CalibrationAdjustmentHint hint,
            CalibrationConstraintDisplay constraintDisplay
    ) {
        Objects.requireNonNull(keyResolver);
        return new CalibrationAdjustmentRule(
                displayId,
                matcher,
                (candidate, existing) -> matcher.test(existing)
                        && Objects.equals(keyResolver.apply(candidate), keyResolver.apply(existing)),
                hint,
                DuplicatePolicy.UNIQUE_KEY,
                constraintDisplay
        );
    }

    public String displayId() {
        return displayId;
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

    public DuplicatePolicy duplicatePolicy() {
        return duplicatePolicy;
    }

    public CalibrationConstraintDisplay constraintDisplay() {
        return constraintDisplay;
    }

    /**
     * JEI などの候補一覧は実際の受理判定から生成し、datapack や追加 MOD の Item も同じ規則で追従させる。
     */
    public @NotNull List<ItemStack> collectDisplayCandidates() {
        return BuiltInRegistries.ITEM.stream()
                .map(item -> item.getDefaultInstance().copyWithCount(1))
                .filter(this::accepts)
                .sorted(Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()))
                .toList();
    }
}
