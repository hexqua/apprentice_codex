package jp.aquafactory.apprenticecodex.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

/** 調整対象 Item が受け付ける候補ルールの順序付き宣言。 */
public final class CalibrationAdjustmentProfile {
    private final List<CalibrationAdjustmentRule> rules;

    private CalibrationAdjustmentProfile(List<CalibrationAdjustmentRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static CalibrationAdjustmentProfile of(CalibrationAdjustmentRule... rules) {
        return new CalibrationAdjustmentProfile(List.of(rules));
    }

    public List<CalibrationAdjustmentRule> rules() {
        return rules;
    }

    boolean canPlace(
            SpellCalibrationAdjustmentTarget target,
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack candidate
    ) {
        return canPlace(
                target,
                targetStack,
                slot,
                candidate,
                existingSlot -> target.getCalibrationAdjustment(targetStack, existingSlot)
        );
    }

    boolean canPlace(
            SpellCalibrationAdjustmentTarget target,
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack candidate,
            @NotNull HolderLookup.Provider lookupProvider
    ) {
        return canPlace(
                target,
                targetStack,
                slot,
                candidate,
                existingSlot -> target.getCalibrationAdjustment(targetStack, existingSlot, lookupProvider)
        );
    }

    private boolean canPlace(
            SpellCalibrationAdjustmentTarget target,
            @NotNull ItemStack targetStack,
            int slot,
            @NotNull ItemStack candidate,
            IntFunction<ItemStack> existingAdjustment
    ) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(existingAdjustment);
        if (slot < 0 || slot >= target.getCalibrationAdjustmentSlotCount(targetStack)) {
            return false;
        }
        if (candidate.isEmpty()) {
            return true;
        }

        var matchedRule = rules.stream().filter(rule -> rule.accepts(candidate)).findFirst().orElse(null);
        if (matchedRule == null) {
            return false;
        }
        for (var existingSlot = 0;
             existingSlot < target.getCalibrationAdjustmentSlotCount(targetStack);
             ++existingSlot) {
            if (existingSlot == slot) {
                continue;
            }
            if (matchedRule.conflicts(candidate, existingAdjustment.apply(existingSlot))) {
                return false;
            }
        }
        return true;
    }
}
