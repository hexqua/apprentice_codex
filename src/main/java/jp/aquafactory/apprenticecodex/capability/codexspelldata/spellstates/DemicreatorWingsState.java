package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public class DemicreatorWingsState implements ICodexSpellState {
    public boolean active;
    public int coreEntityId = -1;
    public int wingEntityId = -1;
    public boolean grantedFlight;

    public void reset() {
        active = false;
        coreEntityId = -1;
        wingEntityId = -1;
        grantedFlight = false;
    }

    @Override
    public CompoundTag save() {
        // ログイン/ログアウトを跨いだ復元は不要なため、動的状態は保持しない。
        return new CompoundTag();
    }

    @Override
    public void load(CompoundTag tag) {
        reset();
    }
}
