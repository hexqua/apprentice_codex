package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public class ThermalSliceState implements ICodexSpellState {
    public int totalTicks;
    public int elapsedTicks;
    public double startX;
    public double startZ;
    public double targetX;
    public double targetZ;
    public int bladeEntityId = -1;

    public boolean isActive() {
        return totalTicks > 0 && elapsedTicks < totalTicks;
    }

    public void reset() {
        totalTicks = 0;
        elapsedTicks = 0;
        startX = 0.0D;
        startZ = 0.0D;
        targetX = 0.0D;
        targetZ = 0.0D;
        bladeEntityId = -1;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putInt("totalTicks", totalTicks);
        tag.putInt("elapsedTicks", elapsedTicks);
        tag.putDouble("startX", startX);
        tag.putDouble("startZ", startZ);
        tag.putDouble("targetX", targetX);
        tag.putDouble("targetZ", targetZ);
        tag.putInt("bladeEntityId", bladeEntityId);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        totalTicks = tag.getInt("totalTicks");
        elapsedTicks = tag.getInt("elapsedTicks");
        startX = tag.getDouble("startX");
        startZ = tag.getDouble("startZ");
        targetX = tag.getDouble("targetX");
        targetZ = tag.getDouble("targetZ");
        bladeEntityId = tag.contains("bladeEntityId") ? tag.getInt("bladeEntityId") : -1;
    }
}
