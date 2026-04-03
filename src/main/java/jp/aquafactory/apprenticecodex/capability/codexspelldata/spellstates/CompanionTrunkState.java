package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CompanionTrunkState implements ICodexSpellState {
    public boolean active;
    public double maxHealth;
    private @Nullable UUID trunkUuid;

    public @Nullable UUID getTrunkUuid() {
        return trunkUuid;
    }

    public void setTrunkUuid(@Nullable UUID trunkUuid) {
        this.trunkUuid = trunkUuid;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("active", active);
        tag.putDouble("maxHealth", maxHealth);
        if (trunkUuid != null) {
            tag.putUUID("trunkUuid", trunkUuid);
        }
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        active = tag.getBoolean("active");
        maxHealth = tag.contains("maxHealth") ? tag.getDouble("maxHealth") : 0.0;
        trunkUuid = tag.hasUUID("trunkUuid") ? tag.getUUID("trunkUuid") : null;
    }
}
