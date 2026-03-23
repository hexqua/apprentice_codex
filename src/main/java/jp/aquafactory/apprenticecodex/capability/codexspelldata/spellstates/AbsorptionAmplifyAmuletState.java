package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public class AbsorptionAmplifyAmuletState implements ICodexSpellState {
    public boolean initialized;
    public float targetAbsorption;
    public float lastKnownAbsorption;
    public long recoveryResumeGameTime;
    public long nextRecoveryGameTime;
    public long nextProcGameTime;
    public boolean recoveryManaPending;
    public float pendingRecoveryAbsorption;
    public long pendingRecoveryConfirmGameTime;

    public void reset() {
        initialized = false;
        targetAbsorption = 0f;
        lastKnownAbsorption = 0f;
        recoveryResumeGameTime = 0L;
        nextRecoveryGameTime = 0L;
        nextProcGameTime = 0L;
        recoveryManaPending = false;
        pendingRecoveryAbsorption = 0f;
        pendingRecoveryConfirmGameTime = 0L;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("initialized", initialized);
        tag.putFloat("targetAbsorption", targetAbsorption);
        tag.putFloat("lastKnownAbsorption", lastKnownAbsorption);
        tag.putLong("recoveryResumeGameTime", recoveryResumeGameTime);
        tag.putLong("nextRecoveryGameTime", nextRecoveryGameTime);
        tag.putLong("nextProcGameTime", nextProcGameTime);
        tag.putBoolean("recoveryManaPending", recoveryManaPending);
        tag.putFloat("pendingRecoveryAbsorption", pendingRecoveryAbsorption);
        tag.putLong("pendingRecoveryConfirmGameTime", pendingRecoveryConfirmGameTime);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        initialized = tag.getBoolean("initialized");
        targetAbsorption = tag.getFloat("targetAbsorption");
        lastKnownAbsorption = tag.getFloat("lastKnownAbsorption");
        recoveryResumeGameTime = tag.getLong("recoveryResumeGameTime");
        nextRecoveryGameTime = tag.getLong("nextRecoveryGameTime");
        nextProcGameTime = tag.getLong("nextProcGameTime");
        recoveryManaPending = tag.getBoolean("recoveryManaPending");
        pendingRecoveryAbsorption = tag.getFloat("pendingRecoveryAbsorption");
        pendingRecoveryConfirmGameTime = tag.getLong("pendingRecoveryConfirmGameTime");
    }
}
