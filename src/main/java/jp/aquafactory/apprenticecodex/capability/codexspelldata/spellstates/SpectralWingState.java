package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public class SpectralWingState implements ICodexSpellState {
    public boolean active;
    public boolean startedBySpell;
    public int launchGraceTicks;
    public int waterGraceTicks;

    public void reset() {
        active = false;
        startedBySpell = false;
        launchGraceTicks = 0;
        waterGraceTicks = 0;
    }

    @Override
    public CompoundTag save() {
        return new CompoundTag();
    }

    @Override
    public void load(CompoundTag tag) {
        reset();
    }
}
