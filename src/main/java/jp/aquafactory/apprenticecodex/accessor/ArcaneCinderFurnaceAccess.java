package jp.aquafactory.apprenticecodex.accessor;

public interface ArcaneCinderFurnaceAccess {
    boolean apprenticeCodex$isArcaneCinderFuelActive();

    void apprenticeCodex$setArcaneCinderFuelActive(boolean active);

    int apprenticeCodex$getLitTime();

    void apprenticeCodex$setLitTime(int litTime);

    int apprenticeCodex$getCookingProgress();

    void apprenticeCodex$setCookingProgress(int cookingProgress);

    int apprenticeCodex$getCookingTotalTime();
}
