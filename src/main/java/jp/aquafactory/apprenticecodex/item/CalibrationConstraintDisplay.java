package jp.aquafactory.apprenticecodex.item;

import java.util.Objects;
import java.util.Optional;

/** 調整ルールの内部判定とは独立した、プレイヤー向け制限文の指定。 */
public record CalibrationConstraintDisplay(Optional<String> translationKey) {
    private static final String SAME_EFFECT_KEY =
            "jei.apprenticecodex.spell_calibration_bench.constraint.same_effect";
    private static final CalibrationConstraintDisplay NONE =
            new CalibrationConstraintDisplay(Optional.empty());
    private static final CalibrationConstraintDisplay SAME_EFFECT =
            translatable(SAME_EFFECT_KEY);

    public CalibrationConstraintDisplay {
        Objects.requireNonNull(translationKey);
        translationKey.ifPresent(key -> {
            if (key.isBlank()) {
                throw new IllegalArgumentException("Calibration constraint translation key must not be blank.");
            }
        });
    }

    public static CalibrationConstraintDisplay none() {
        return NONE;
    }

    public static CalibrationConstraintDisplay sameEffect() {
        return SAME_EFFECT;
    }

    public static CalibrationConstraintDisplay translatable(String translationKey) {
        return new CalibrationConstraintDisplay(Optional.of(Objects.requireNonNull(translationKey)));
    }
}
