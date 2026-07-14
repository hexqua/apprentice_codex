package jp.aquafactory.apprenticecodex.item;

public enum SpellCalibrationImbueState {
    REJECTED(false, false),
    ACCEPTED_USABLE(true, true),
    ACCEPTED_CURRENTLY_UNUSABLE(true, false);

    private final boolean insertable;
    private final boolean usable;

    SpellCalibrationImbueState(boolean insertable, boolean usable) {
        this.insertable = insertable;
        this.usable = usable;
    }

    public boolean canInsert() {
        return insertable;
    }

    public boolean isUsable() {
        return usable;
    }

    public static SpellCalibrationImbueState accepted(boolean usable) {
        return usable ? ACCEPTED_USABLE : ACCEPTED_CURRENTLY_UNUSABLE;
    }
}
