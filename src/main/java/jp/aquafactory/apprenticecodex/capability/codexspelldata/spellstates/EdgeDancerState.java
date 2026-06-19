package jp.aquafactory.apprenticecodex.capability.codexspelldata.spellstates;

import jp.aquafactory.apprenticecodex.capability.codexspelldata.ICodexSpellState;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EdgeDancerState implements ICodexSpellState {
    private static final HolderLookup.Provider SERIALIZATION_LOOKUP =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static final String ACTIVE_TAG = "Active";
    private static final String INSTANCE_ID_TAG = "InstanceId";
    private static final String STORED_OFFHAND_STACK_TAG = "StoredOffhandStack";
    private static final String HAD_STORED_OFFHAND_TAG = "HadStoredOffhand";

    public boolean active;
    private @Nullable UUID instanceId;
    private ItemStack storedOffhandStack = ItemStack.EMPTY;
    private boolean hadStoredOffhand;

    public @Nullable UUID getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(@Nullable UUID instanceId) {
        this.instanceId = instanceId;
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

    public boolean hadStoredOffhand() {
        return hadStoredOffhand;
    }

    public void setHadStoredOffhand(boolean hadStoredOffhand) {
        this.hadStoredOffhand = hadStoredOffhand;
    }

    public void reset() {
        active = false;
        instanceId = null;
        storedOffhandStack = ItemStack.EMPTY;
        hadStoredOffhand = false;
    }

    @Override
    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putBoolean(ACTIVE_TAG, active);
        if (instanceId != null) {
            tag.putUUID(INSTANCE_ID_TAG, instanceId);
        }
        if (!storedOffhandStack.isEmpty()) {
            tag.put(STORED_OFFHAND_STACK_TAG, storedOffhandStack.saveOptional(SERIALIZATION_LOOKUP));
        }
        tag.putBoolean(HAD_STORED_OFFHAND_TAG, hadStoredOffhand);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        active = tag.getBoolean(ACTIVE_TAG);
        instanceId = tag.hasUUID(INSTANCE_ID_TAG) ? tag.getUUID(INSTANCE_ID_TAG) : null;
        storedOffhandStack = tag.contains(STORED_OFFHAND_STACK_TAG)
                ? ItemStack.parseOptional(SERIALIZATION_LOOKUP, tag.getCompound(STORED_OFFHAND_STACK_TAG))
                : ItemStack.EMPTY;
        hadStoredOffhand = tag.getBoolean(HAD_STORED_OFFHAND_TAG);
    }
}
