package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public final class UndyingEmblemState implements ICodexSpellState {
    private int remainingCooldownTicks;

    public int getRemainingCooldownTicks() {
        return remainingCooldownTicks;
    }

    public void setRemainingCooldownTicks(int remainingCooldownTicks) {
        this.remainingCooldownTicks = Math.max(remainingCooldownTicks, 0);
    }

    public void reset() {
        remainingCooldownTicks = 0;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("remainingCooldownTicks", remainingCooldownTicks);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        setRemainingCooldownTicks(tag.getInt("remainingCooldownTicks"));
    }
}
