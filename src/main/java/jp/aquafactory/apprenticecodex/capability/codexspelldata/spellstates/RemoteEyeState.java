package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public class RemoteEyeState implements ICodexSpellState {
    public long activeUntilGameTime;
    public double anchorX;
    public double anchorY;
    public double anchorZ;
    public float anchorYaw;
    public float anchorPitch;

    public void reset() {
        activeUntilGameTime = 0L;
        anchorX = 0.0;
        anchorY = 0.0;
        anchorZ = 0.0;
        anchorYaw = 0.0f;
        anchorPitch = 0.0f;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putLong("activeUntilGameTime", activeUntilGameTime);
        tag.putDouble("anchorX", anchorX);
        tag.putDouble("anchorY", anchorY);
        tag.putDouble("anchorZ", anchorZ);
        tag.putFloat("anchorYaw", anchorYaw);
        tag.putFloat("anchorPitch", anchorPitch);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        activeUntilGameTime = tag.getLong("activeUntilGameTime");
        anchorX = tag.getDouble("anchorX");
        anchorY = tag.getDouble("anchorY");
        anchorZ = tag.getDouble("anchorZ");
        anchorYaw = tag.contains("anchorYaw") ? tag.getFloat("anchorYaw") : 0.0f;
        anchorPitch = tag.contains("anchorPitch") ? tag.getFloat("anchorPitch") : 0.0f;
    }
}
