package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public final class FocusStaffbowCastState implements ICodexSpellState {
    public String spellId = "";
    public int spellLevel;
    public String castSource = CastSource.NONE.name();
    public String castingSlot = "";
    public long startedGameTime;
    public int requiredCastTicks;
    public String dimensionId = "";
    public int selectedHotbarSlot = -1;
    public boolean preCastStarted;

    public boolean isActive() {
        return !spellId.isEmpty() && requiredCastTicks > 0;
    }

    public void start(AbstractSpell spell, int spellLevel, CastSource castSource, String castingSlot, long startedGameTime,
                      int requiredCastTicks, String dimensionId, int selectedHotbarSlot) {
        this.spellId = spell.getSpellId();
        this.spellLevel = spellLevel;
        this.castSource = castSource.name();
        this.castingSlot = castingSlot;
        this.startedGameTime = startedGameTime;
        this.requiredCastTicks = requiredCastTicks;
        this.dimensionId = dimensionId;
        this.selectedHotbarSlot = selectedHotbarSlot;
        this.preCastStarted = false;
    }

    public void markPreCastStarted() {
        preCastStarted = true;
    }

    public boolean matches(AbstractSpell spell, int spellLevel, CastSource castSource, String castingSlot) {
        return this.spellId.equals(spell.getSpellId())
                && this.spellLevel == spellLevel
                && this.castSource.equals(castSource.name())
                && this.castingSlot.equals(castingSlot);
    }

    public boolean isReady(long gameTime) {
        return isActive() && gameTime - startedGameTime >= requiredCastTicks;
    }

    public long getElapsedTicks(long gameTime) {
        return Math.max(0L, gameTime - startedGameTime);
    }

    public long getOverchargeTicks(long gameTime) {
        return Math.max(0L, getElapsedTicks(gameTime) - requiredCastTicks);
    }

    public void reset() {
        spellId = "";
        spellLevel = 0;
        castSource = CastSource.NONE.name();
        castingSlot = "";
        startedGameTime = 0L;
        requiredCastTicks = 0;
        dimensionId = "";
        selectedHotbarSlot = -1;
        preCastStarted = false;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putString("spellId", spellId);
        tag.putInt("spellLevel", spellLevel);
        tag.putString("castSource", castSource);
        tag.putString("castingSlot", castingSlot);
        tag.putLong("startedGameTime", startedGameTime);
        tag.putInt("requiredCastTicks", requiredCastTicks);
        tag.putString("dimensionId", dimensionId);
        tag.putInt("selectedHotbarSlot", selectedHotbarSlot);
        tag.putBoolean("preCastStarted", preCastStarted);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        spellId = tag.getString("spellId");
        spellLevel = tag.getInt("spellLevel");
        castSource = tag.contains("castSource") ? tag.getString("castSource") : CastSource.NONE.name();
        castingSlot = tag.contains("castingSlot") ? tag.getString("castingSlot") : "";
        startedGameTime = tag.getLong("startedGameTime");
        requiredCastTicks = tag.getInt("requiredCastTicks");
        dimensionId = tag.getString("dimensionId");
        selectedHotbarSlot = tag.contains("selectedHotbarSlot") ? tag.getInt("selectedHotbarSlot") : -1;
        preCastStarted = tag.getBoolean("preCastStarted");
    }
}
