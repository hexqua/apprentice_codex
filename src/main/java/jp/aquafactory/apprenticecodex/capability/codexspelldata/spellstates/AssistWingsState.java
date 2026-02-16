package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public class AssistWingsState implements ICodexSpellState {

    public int doneJump;
    public int localEntityId = -1; // 永続化しない.

    @Override
    public CompoundTag save() {
        var t = new CompoundTag();
        t.putInt("doneJump", doneJump);
        return t;
    }

    @Override
    public void load(CompoundTag tag) {
        doneJump = tag.getInt("doneJump");
    }
}
