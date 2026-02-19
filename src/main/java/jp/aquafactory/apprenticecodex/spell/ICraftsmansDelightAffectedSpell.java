package jp.aquafactory.apprenticecodex.spell;

public interface ICraftsmansDelightAffectedSpell {
    default boolean isCraftsmansDelightBreakSpeedBonusEnabled() {
        return true;
    }

    default boolean isCraftsmansDelightManaCostDiscountEnabled() {
        return true;
    }
}
