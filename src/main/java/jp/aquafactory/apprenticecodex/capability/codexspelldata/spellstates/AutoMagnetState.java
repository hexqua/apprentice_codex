package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AutoMagnetState implements ICodexSpellState {
    public boolean active;
    public double range;
    public double collectMana;
    private @Nullable UUID familiarUuid;

    public @Nullable UUID getFamiliarUuid() {
        return familiarUuid;
    }

    public void setFamiliarUuid(@Nullable UUID familiarUuid) {
        this.familiarUuid = familiarUuid;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("active", active);
        tag.putDouble("range", range);
        tag.putDouble("collectMana", collectMana);
        if (familiarUuid != null) {
            tag.putUUID("familiarUuid", familiarUuid);
        }
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        active = tag.getBoolean("active");
        range = tag.contains("range") ? tag.getDouble("range") : 0.0;
        collectMana = tag.contains("collectMana") ? tag.getDouble("collectMana") : 0.0;
        familiarUuid = tag.hasUUID("familiarUuid") ? tag.getUUID("familiarUuid") : null;
    }
}
