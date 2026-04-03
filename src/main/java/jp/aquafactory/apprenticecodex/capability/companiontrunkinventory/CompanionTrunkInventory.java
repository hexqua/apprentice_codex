package jp.aquafactory.apprenticecodex.capability.companiontrunkinventory;

import jp.aquafactory.apprenticecodex.ApprenticeCodex;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class CompanionTrunkInventory {
    public static final int SIZE = 27;

    private final ItemStackHandler handler = new ItemStackHandler(SIZE);
    private @Nullable String customName;

    public ItemStackHandler getHandler() {
        return handler;
    }

    public boolean isEmpty() {
        for (var i = 0; i < handler.getSlots(); ++i) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void clearItems() {
        for (var i = 0; i < handler.getSlots(); ++i) {
            handler.setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }

    public @Nullable Component getCustomName() {
        return customName == null || customName.isBlank() ? null : Component.literal(customName);
    }

    public void setCustomName(@Nullable Component name) {
        if (name == null) {
            customName = null;
            return;
        }

        var text = name.getString();
        customName = text.isBlank() ? null : text;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        tag.put("Items", handler.serializeNBT(provider));
        if (customName != null && !customName.isBlank()) {
            tag.putString("CustomName", customName);
        }
        return tag;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        try {
            handler.deserializeNBT(provider, nbt.getCompound("Items"));
        } catch (Exception e) {
            ApprenticeCodex.LOGGER.error("Failed to load CompanionTrunk inventory data, resetting.", e);
            handler.deserializeNBT(provider, new CompoundTag());
        }
        customName = nbt.contains("CustomName") ? nbt.getString("CustomName") : null;
    }

    public void copyAllFrom(HolderLookup.Provider provider, CompanionTrunkInventory other) {
        deserializeNBT(provider, other.serializeNBT(provider));
    }

    public void copyNameOnlyFrom(CompanionTrunkInventory other) {
        clearItems();
        setCustomName(other.getCustomName());
    }
}
