package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public class ForceFieldState implements ICodexSpellState {
    public static final int INTERCEPT_KIND_NONE = 0;
    public static final int INTERCEPT_KIND_PROJECTILE = 1;
    public static final int INTERCEPT_KIND_MELEE = 2;

    public boolean hasInterceptPoint;
    public double lastInterceptX;
    public double lastInterceptY;
    public double lastInterceptZ;
    public long lastInterceptGameTime;
    public int lastInterceptKind = INTERCEPT_KIND_NONE;

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("hasInterceptPoint", hasInterceptPoint);
        tag.putDouble("lastInterceptX", lastInterceptX);
        tag.putDouble("lastInterceptY", lastInterceptY);
        tag.putDouble("lastInterceptZ", lastInterceptZ);
        tag.putLong("lastInterceptGameTime", lastInterceptGameTime);
        tag.putInt("lastInterceptKind", lastInterceptKind);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        hasInterceptPoint = tag.getBoolean("hasInterceptPoint");
        lastInterceptX = tag.getDouble("lastInterceptX");
        lastInterceptY = tag.getDouble("lastInterceptY");
        lastInterceptZ = tag.getDouble("lastInterceptZ");
        lastInterceptGameTime = tag.getLong("lastInterceptGameTime");
        lastInterceptKind = tag.contains("lastInterceptKind") ? tag.getInt("lastInterceptKind") : INTERCEPT_KIND_NONE;
    }
}
