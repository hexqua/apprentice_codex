package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public class FeatherRushState implements ICodexSpellState {
    public long activeUntilGameTime;
    public int wingEntityId = -1;
    public boolean noGravityApplied;

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putLong("activeUntilGameTime", activeUntilGameTime);
        tag.putInt("wingEntityId", wingEntityId);
        tag.putBoolean("noGravityApplied", noGravityApplied);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        activeUntilGameTime = tag.getLong("activeUntilGameTime");
        wingEntityId = tag.getInt("wingEntityId");
        noGravityApplied = tag.getBoolean("noGravityApplied");
    }
}
