package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ServantGazeState implements ICodexSpellState {
    public boolean active;
    public int spellLevel;
    public float damage;
    public double radius;
    public int attackManaCost;
    private @Nullable UUID staffUuid;

    public @Nullable UUID getStaffUuid() {
        return staffUuid;
    }

    public void setStaffUuid(@Nullable UUID staffUuid) {
        this.staffUuid = staffUuid;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean("active", active);
        tag.putInt("spellLevel", spellLevel);
        tag.putFloat("damage", damage);
        tag.putDouble("radius", radius);
        tag.putInt("attackManaCost", attackManaCost);
        if (staffUuid != null) tag.putUUID("staffUuid", staffUuid);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        active = tag.getBoolean("active");
        spellLevel = tag.getInt("spellLevel");
        damage = tag.getFloat("damage");
        radius = tag.getDouble("radius");
        attackManaCost = tag.getInt("attackManaCost");
        staffUuid = tag.hasUUID("staffUuid") ? tag.getUUID("staffUuid") : null;
    }
}
