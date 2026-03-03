package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AutoMagnetState implements ICodexSpellState {
    public boolean active;
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
        if (familiarUuid != null) {
            tag.putUUID("familiarUuid", familiarUuid);
        }
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        active = tag.getBoolean("active");
        familiarUuid = tag.hasUUID("familiarUuid") ? tag.getUUID("familiarUuid") : null;
    }
}
