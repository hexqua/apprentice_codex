package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public class MantisLeapState implements ICodexSpellState {
    public int totalTicks;
    public int elapsedTicks;
    public double startX;
    public double startY;
    public double startZ;
    public double targetX;
    public double targetY;
    public double targetZ;
    public double arcHeight;
    public boolean noGravityApplied;

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("totalTicks", totalTicks);
        tag.putInt("elapsedTicks", elapsedTicks);
        tag.putDouble("startX", startX);
        tag.putDouble("startY", startY);
        tag.putDouble("startZ", startZ);
        tag.putDouble("targetX", targetX);
        tag.putDouble("targetY", targetY);
        tag.putDouble("targetZ", targetZ);
        tag.putDouble("arcHeight", arcHeight);
        tag.putBoolean("noGravityApplied", noGravityApplied);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        totalTicks = tag.getInt("totalTicks");
        elapsedTicks = tag.getInt("elapsedTicks");
        startX = tag.getDouble("startX");
        startY = tag.getDouble("startY");
        startZ = tag.getDouble("startZ");
        targetX = tag.getDouble("targetX");
        targetY = tag.getDouble("targetY");
        targetZ = tag.getDouble("targetZ");
        arcHeight = tag.getDouble("arcHeight");
        noGravityApplied = tag.getBoolean("noGravityApplied");
    }
}
