package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public class MirageAvoidanceState implements ICodexSpellState {
    public long startGameTime;
    public long activeUntilGameTime;
    public long invulnerableUntilGameTime;
    public float movementForward;
    public float movementStrafe;
    public boolean suppressFallDamageUntilGround;

    public void reset() {
        startGameTime = 0L;
        activeUntilGameTime = 0L;
        invulnerableUntilGameTime = 0L;
        movementForward = 0.0F;
        movementStrafe = 0.0F;
        suppressFallDamageUntilGround = false;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putLong("startGameTime", startGameTime);
        tag.putLong("activeUntilGameTime", activeUntilGameTime);
        tag.putLong("invulnerableUntilGameTime", invulnerableUntilGameTime);
        tag.putFloat("movementForward", movementForward);
        tag.putFloat("movementStrafe", movementStrafe);
        tag.putBoolean("suppressFallDamageUntilGround", suppressFallDamageUntilGround);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        startGameTime = tag.getLong("startGameTime");
        activeUntilGameTime = tag.getLong("activeUntilGameTime");
        invulnerableUntilGameTime = tag.getLong("invulnerableUntilGameTime");
        movementForward = tag.getFloat("movementForward");
        movementStrafe = tag.getFloat("movementStrafe");
        suppressFallDamageUntilGround = tag.getBoolean("suppressFallDamageUntilGround");
    }
}
