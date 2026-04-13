package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class HealingBloomState implements ICodexSpellState {
    private @Nullable UUID bloomUuid;

    public @Nullable UUID getBloomUuid() {
        return bloomUuid;
    }

    public void setBloomUuid(@Nullable UUID bloomUuid) {
        this.bloomUuid = bloomUuid;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        if (bloomUuid != null) {
            tag.putUUID("bloomUuid", bloomUuid);
        }
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        bloomUuid = tag.hasUUID("bloomUuid") ? tag.getUUID("bloomUuid") : null;
    }
}
