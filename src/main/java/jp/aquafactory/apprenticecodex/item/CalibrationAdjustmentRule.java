package jp.aquafactory.apprenticecodex.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
    private final Supplier<List<Component>> effectLinesSupplier;

    private CalibrationAdjustmentRule(
            String displayId,
            Predicate<ItemStack> matcher,
            BiPredicate<ItemStack, ItemStack> conflict,
            CalibrationAdjustmentHint hint,
            DuplicatePolicy duplicatePolicy,
            CalibrationConstraintDisplay constraintDisplay,
            Supplier<List<Component>> effectLinesSupplier
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
        this.effectLinesSupplier = Objects.requireNonNull(effectLinesSupplier);
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
                CalibrationConstraintDisplay.none(),
                List::of
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
                constraintDisplay,
                List::of
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
                constraintDisplay,
                List::of
        );
    }

    public CalibrationAdjustmentRule withEffectLines(List<Component> effectLines) {
        var copiedLines = List.copyOf(effectLines);
        return withEffectLines(() -> copiedLines);
    }

    public CalibrationAdjustmentRule withEffectLines(Supplier<List<Component>> effectLinesSupplier) {
        return new CalibrationAdjustmentRule(
                displayId,
                matcher,
                conflict,
                hint,
                duplicatePolicy,
                constraintDisplay,
                effectLinesSupplier
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

    public List<Component> effectLines() {
        var effectLines = List.copyOf(Objects.requireNonNull(effectLinesSupplier.get()));
        if (effectLines.isEmpty() || effectLines.size() > 3) {
            throw new IllegalStateException("Calibration adjustment effect must contain one to three lines: " + displayId);
        }
        return effectLines;
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
