package jp.aquafactory.apprenticecodex.item.focusstaffbow;

public final class FocusStaffbowClientLoanState {
    private static final float LOAN_EPSILON = 1.0e-4F;

    private static float remainingLoanMana;

    private FocusStaffbowClientLoanState() {
    }

    public static void applySyncedState(float syncedRemainingLoanMana) {
        remainingLoanMana = Math.max(0.0F, syncedRemainingLoanMana);
    }

    public static boolean hasOutstandingLoan() {
        return remainingLoanMana > LOAN_EPSILON;
    }

    public static float remainingLoanMana() {
        return remainingLoanMana;
    }

    public static void clear() {
        remainingLoanMana = 0.0F;
    }
}
