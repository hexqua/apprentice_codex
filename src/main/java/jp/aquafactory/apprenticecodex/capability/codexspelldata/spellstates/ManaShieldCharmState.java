package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public class ManaShieldCharmState implements ICodexSpellState {
    public boolean cooldownActive;
    public boolean manualReentryGuard;

    public void reset() {
        cooldownActive = false;
        manualReentryGuard = false;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("cooldownActive", cooldownActive);
        tag.putBoolean("manualReentryGuard", manualReentryGuard);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        cooldownActive = tag.getBoolean("cooldownActive");
        manualReentryGuard = tag.getBoolean("manualReentryGuard");
    }
}
