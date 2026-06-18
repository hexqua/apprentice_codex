package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import jp.aquafactory.apprenticecodex.spell.automagnet.AutoMagnetCollectionMode;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AutoMagnetState implements ICodexSpellState {
    public boolean active;
    public double range;
    public double collectMana;
    private AutoMagnetCollectionMode collectionMode = AutoMagnetCollectionMode.NORMAL;
    private @Nullable UUID familiarUuid;

    public AutoMagnetCollectionMode getCollectionMode() {
        return collectionMode;
    }

    public void setCollectionMode(AutoMagnetCollectionMode collectionMode) {
        this.collectionMode = collectionMode;
    }

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
        tag.putString("collectionMode", collectionMode.name());
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
        collectionMode = tag.contains("collectionMode")
                ? AutoMagnetCollectionMode.byName(tag.getString("collectionMode"))
                : AutoMagnetCollectionMode.NORMAL;
        familiarUuid = tag.hasUUID("familiarUuid") ? tag.getUUID("familiarUuid") : null;
    }
}
