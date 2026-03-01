package jp.aquafactory.apprenticecodex.capability.personalinventory;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.items.ItemStackHandler;

public class PersonalInventory {
    public static final int MAX_SIZE = 54;
    private final ItemStackHandler handler = new ItemStackHandler(MAX_SIZE);

    public ItemStackHandler getHandler() {
        return handler;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return handler.serializeNBT(provider);
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        try {
            handler.deserializeNBT(provider, nbt);
        } catch (Exception e) {
            ApprenticeCodex.LOGGER.error("Failed to load PersonalInventory data, resetting.", e);
            handler.deserializeNBT(provider, new CompoundTag());
        }
    }
}
