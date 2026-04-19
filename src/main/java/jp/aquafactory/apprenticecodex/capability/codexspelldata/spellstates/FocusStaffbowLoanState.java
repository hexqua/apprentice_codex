package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public final class FocusStaffbowLoanState implements ICodexSpellState {
    private static final String REMAINING_LOAN_MANA_TAG = "RemainingLoanMana";

    public float remainingLoanMana;

    public boolean hasOutstandingLoan() {
        return remainingLoanMana > 0.0F;
    }

    public void addLoan(float loanMana) {
        remainingLoanMana = Math.max(0.0F, remainingLoanMana + Math.max(0.0F, loanMana));
    }

    public void repay(float repaidMana) {
        remainingLoanMana = Math.max(0.0F, remainingLoanMana - Math.max(0.0F, repaidMana));
    }

    public void reset() {
        remainingLoanMana = 0.0F;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putFloat(REMAINING_LOAN_MANA_TAG, remainingLoanMana);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        remainingLoanMana = Math.max(0.0F, tag.getFloat(REMAINING_LOAN_MANA_TAG));
    }
}
