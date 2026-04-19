package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;

public final class FocusStaffbowCastState implements ICodexSpellState {
    public enum Mode {
        NONE,
        PENDING,
        CONTINUOUS
    }

    public String spellId = "";
    public int spellLevel;
    public String castSource = CastSource.NONE.name();
    public String castingSlot = "";
    public long startedGameTime;
    public int requiredCastTicks;
    public int chargeBaselineTicks;
    public String dimensionId = "";
    public int selectedHotbarSlot = -1;
    public String mode = Mode.NONE.name();
    public int chargeUpdateIntervalTicks = 1;
    public long lastChargeSampledTicks = Long.MIN_VALUE;
    public boolean preCastStarted;

    public boolean isActive() {
        return !spellId.isEmpty() && getMode() != Mode.NONE && requiredCastTicks >= 0;
    }

    public void startPending(AbstractSpell spell, int spellLevel, CastSource castSource, String castingSlot, long startedGameTime,
                             int requiredCastTicks, int chargeBaselineTicks, String dimensionId, int selectedHotbarSlot) {
        startInternal(spell, spellLevel, castSource, castingSlot, startedGameTime, requiredCastTicks,
                chargeBaselineTicks, dimensionId, selectedHotbarSlot, Mode.PENDING, 1);
    }

    public void startContinuous(AbstractSpell spell, int spellLevel, CastSource castSource, String castingSlot, long startedGameTime,
                                int requiredCastTicks, String dimensionId, int selectedHotbarSlot, int chargeUpdateIntervalTicks) {
        startInternal(spell, spellLevel, castSource, castingSlot, startedGameTime, requiredCastTicks,
                requiredCastTicks, dimensionId, selectedHotbarSlot, Mode.CONTINUOUS, chargeUpdateIntervalTicks);
        this.preCastStarted = true;
    }

    private void startInternal(AbstractSpell spell, int spellLevel, CastSource castSource, String castingSlot, long startedGameTime,
                               int requiredCastTicks, int chargeBaselineTicks, String dimensionId, int selectedHotbarSlot,
                               Mode mode, int chargeUpdateIntervalTicks) {
        this.spellId = spell.getSpellId();
        this.spellLevel = spellLevel;
        this.castSource = castSource.name();
        this.castingSlot = castingSlot;
        this.startedGameTime = startedGameTime;
        this.requiredCastTicks = Math.max(0, requiredCastTicks);
        this.chargeBaselineTicks = Math.max(0, chargeBaselineTicks);
        this.dimensionId = dimensionId;
        this.selectedHotbarSlot = selectedHotbarSlot;
        this.mode = mode.name();
        this.chargeUpdateIntervalTicks = Math.max(1, chargeUpdateIntervalTicks);
        this.lastChargeSampledTicks = Long.MIN_VALUE;
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

    public Mode getMode() {
        try {
            return Mode.valueOf(mode);
        } catch (IllegalArgumentException ignored) {
            return Mode.NONE;
        }
    }

    public boolean isPending() {
        return isActive() && getMode() == Mode.PENDING;
    }

    public boolean isContinuous() {
        return isActive() && getMode() == Mode.CONTINUOUS;
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
        chargeBaselineTicks = 0;
        dimensionId = "";
        selectedHotbarSlot = -1;
        mode = Mode.NONE.name();
        chargeUpdateIntervalTicks = 1;
        lastChargeSampledTicks = Long.MIN_VALUE;
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
        tag.putInt("chargeBaselineTicks", chargeBaselineTicks);
        tag.putString("dimensionId", dimensionId);
        tag.putInt("selectedHotbarSlot", selectedHotbarSlot);
        tag.putString("mode", mode);
        tag.putInt("chargeUpdateIntervalTicks", chargeUpdateIntervalTicks);
        tag.putLong("lastChargeSampledTicks", lastChargeSampledTicks);
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
        chargeBaselineTicks = tag.contains("chargeBaselineTicks") ? Math.max(0, tag.getInt("chargeBaselineTicks")) : requiredCastTicks;
        dimensionId = tag.getString("dimensionId");
        selectedHotbarSlot = tag.contains("selectedHotbarSlot") ? tag.getInt("selectedHotbarSlot") : -1;
        mode = tag.contains("mode") ? tag.getString("mode") : Mode.NONE.name();
        chargeUpdateIntervalTicks = tag.contains("chargeUpdateIntervalTicks") ? Math.max(1, tag.getInt("chargeUpdateIntervalTicks")) : 1;
        lastChargeSampledTicks = tag.contains("lastChargeSampledTicks") ? tag.getLong("lastChargeSampledTicks") : Long.MIN_VALUE;
        preCastStarted = tag.getBoolean("preCastStarted");
    }
}
