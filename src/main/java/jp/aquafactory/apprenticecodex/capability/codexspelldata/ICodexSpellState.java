package jp.aquafactory.apprenticecodex.capability.codexspelldata;

import net.minecraft.nbt.CompoundTag;

public interface ICodexSpellState {
    CompoundTag save();
    void load(CompoundTag tag);
}
