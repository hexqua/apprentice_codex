package jp.aquafactory.apprenticecodex.item.spellgun;

import java.util.Objects;

/**
 * 専用サーバーの Spellgun 設定を、クライアント側のツールチップ用に保持する。
 */
public final class SpellgunConfigState {
    private static Values values = Values.defaults();

    private SpellgunConfigState() {
    }

    public static Values values() {
        return values;
    }

    public static void setValues(Values values) {
        SpellgunConfigState.values = Objects.requireNonNull(values);
    }

    public static void reset() {
        values = Values.defaults();
    }

    public record Values(
            int ironMaxInstantImbueCooldownTicks,
            int ironOverriddenSpellCooldownTicks,
            int copperMaxInstantImbueCooldownTicks,
            int copperOverriddenSpellCooldownTicks,
            int goldReducedCooldownMinimumTicks,
            int goldCooldownReductionTicks,
            boolean ironIgnoreMaxMana,
            boolean copperIgnoreMaxMana,
            boolean goldIgnoreMaxMana,
            boolean diamondIgnoreMaxMana
    ) {
        public static Values defaults() {
            return new Values(100, 4, 400, 20, 10, 200, false, false, false, false);
        }
    }
}
