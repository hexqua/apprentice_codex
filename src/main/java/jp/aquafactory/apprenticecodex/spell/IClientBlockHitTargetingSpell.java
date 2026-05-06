package jp.aquafactory.apprenticecodex.spell;

public interface IClientBlockHitTargetingSpell extends IClientBlockTargetingSpell {
    default boolean ignoresClientBlockTargetingRange() {
        return false;
    }
}
