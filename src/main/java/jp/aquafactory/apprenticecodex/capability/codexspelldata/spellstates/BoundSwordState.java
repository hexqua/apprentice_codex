package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BoundSwordState implements ICodexSpellState {
    private static final String ACTIVE_TAG = "Active";
    private static final String INSTANCE_ID_TAG = "InstanceId";
    private static final String STORED_MAINHAND_STACK_TAG = "StoredMainhandStack";
    private static final String DISPLAY_DAMAGE_TAG = "DisplayDamage";

    public boolean active;
    private @Nullable UUID instanceId;
    private ItemStack storedMainhandStack = ItemStack.EMPTY;
    public float displayDamage;

    public @Nullable UUID getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(@Nullable UUID instanceId) {
        this.instanceId = instanceId;
    }

    public ItemStack getStoredMainhandStack() {
        return storedMainhandStack;
    }

    public void setStoredMainhandStack(ItemStack stack) {
        storedMainhandStack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public boolean hasStoredMainhandStack() {
        return !storedMainhandStack.isEmpty();
    }

    public void reset() {
        active = false;
        instanceId = null;
        storedMainhandStack = ItemStack.EMPTY;
        displayDamage = 0.0F;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean(ACTIVE_TAG, active);
        if (instanceId != null) {
            tag.putUUID(INSTANCE_ID_TAG, instanceId);
        }
        if (!storedMainhandStack.isEmpty()) {
            tag.put(STORED_MAINHAND_STACK_TAG, storedMainhandStack.save(new CompoundTag()));
        }
        tag.putFloat(DISPLAY_DAMAGE_TAG, displayDamage);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        active = tag.getBoolean(ACTIVE_TAG);
        instanceId = tag.hasUUID(INSTANCE_ID_TAG) ? tag.getUUID(INSTANCE_ID_TAG) : null;
        storedMainhandStack = tag.contains(STORED_MAINHAND_STACK_TAG)
                ? ItemStack.of(tag.getCompound(STORED_MAINHAND_STACK_TAG))
                : ItemStack.EMPTY;
        displayDamage = tag.getFloat(DISPLAY_DAMAGE_TAG);
    }
}
