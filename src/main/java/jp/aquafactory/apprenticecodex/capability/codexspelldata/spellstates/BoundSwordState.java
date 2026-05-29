package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BoundSwordState implements ICodexSpellState {
    private static final HolderLookup.Provider SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static final String ACTIVE_TAG = "Active";
    private static final String INSTANCE_ID_TAG = "InstanceId";
    private static final String STORED_MAINHAND_STACK_TAG = "StoredMainhandStack";
    private static final String STORED_OFFHAND_STACK_TAG = "StoredOffhandStack";
    private static final String OFFHAND_SWORD_GENERATED_TAG = "OffhandSwordGenerated";
    private static final String DISPLAY_DAMAGE_TAG = "DisplayDamage";

    public boolean active;
    private @Nullable UUID instanceId;
    private ItemStack storedMainhandStack = ItemStack.EMPTY;
    private ItemStack storedOffhandStack = ItemStack.EMPTY;
    private boolean offhandSwordGenerated;
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

    public ItemStack getStoredOffhandStack() {
        return storedOffhandStack;
    }

    public void setStoredOffhandStack(ItemStack stack) {
        storedOffhandStack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public boolean hasStoredOffhandStack() {
        return !storedOffhandStack.isEmpty();
    }

    public boolean isOffhandSwordGenerated() {
        return offhandSwordGenerated;
    }

    public void setOffhandSwordGenerated(boolean offhandSwordGenerated) {
        this.offhandSwordGenerated = offhandSwordGenerated;
    }

    public void reset() {
        active = false;
        instanceId = null;
        storedMainhandStack = ItemStack.EMPTY;
        storedOffhandStack = ItemStack.EMPTY;
        offhandSwordGenerated = false;
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
            tag.put(STORED_MAINHAND_STACK_TAG, storedMainhandStack.saveOptional(SERIALIZATION_LOOKUP));
        }
        if (!storedOffhandStack.isEmpty()) {
            tag.put(STORED_OFFHAND_STACK_TAG, storedOffhandStack.saveOptional(SERIALIZATION_LOOKUP));
        }
        tag.putBoolean(OFFHAND_SWORD_GENERATED_TAG, offhandSwordGenerated);
        tag.putFloat(DISPLAY_DAMAGE_TAG, displayDamage);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        active = tag.getBoolean(ACTIVE_TAG);
        instanceId = tag.hasUUID(INSTANCE_ID_TAG) ? tag.getUUID(INSTANCE_ID_TAG) : null;
        storedMainhandStack = tag.contains(STORED_MAINHAND_STACK_TAG)
                ? ItemStack.parseOptional(SERIALIZATION_LOOKUP, tag.getCompound(STORED_MAINHAND_STACK_TAG))
                : ItemStack.EMPTY;
        storedOffhandStack = tag.contains(STORED_OFFHAND_STACK_TAG)
                ? ItemStack.parseOptional(SERIALIZATION_LOOKUP, tag.getCompound(STORED_OFFHAND_STACK_TAG))
                : ItemStack.EMPTY;
        offhandSwordGenerated = tag.getBoolean(OFFHAND_SWORD_GENERATED_TAG);
        displayDamage = tag.getFloat(DISPLAY_DAMAGE_TAG);
    }
}
